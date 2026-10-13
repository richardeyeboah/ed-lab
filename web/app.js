// Presentation only: all queue ordering, validation and room changes come from Java.
const $ = id => document.getElementById(id);
const titles = ['', 'Admit patients', 'Display rooms', 'Occupied room', 'Full capacity', 'Null patient', 'Discharge', 'Updated rooms', 'Doctor clearance', 'Empty room', 'Search & peek'];
const descriptions = ['', 'Admit the next patients by severity, then admit Lisa directly into room 10.', 'Read the room array and sorted waiting queue without changing anything.', 'Try room 1 again. It is occupied: Java must reject admission and preserve the queue.', 'Fill every remaining room. Patients still waiting cannot replace existing occupants.', 'Pass a null patient. Java must reject the input without changing state.', 'Explicitly discharge the cleared non-critical patients in rooms 6 and 8.', 'Display room occupancy after Test 6: rooms 6 and 8 are available.', 'The doctor has cleared the patients in rooms 1 and 3. Discharge them.', 'Try to discharge room 6 again. It is already empty: Java must reject the action.', 'Search records, inspect the final queue, and peek twice without removing anyone.'];
const levelColors = ['', '#d74842', '#db8633', '#d3ad28', '#416fc3', '#429965'];
const escape = s => String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
let currentCsv='', source='', selected=1, beforeState=null, afterState=null, showingBefore=false, arrivalView=false, codeSection='test', activeRequest=0;
const completed = new Set();
function notice(message) { $('notice').textContent=message; $('notice').hidden=!message; }
async function api(url,options) {
 const response=await fetch(url,options); const data=await response.json();
 if(!response.ok)throw Error(data.error||'Java request failed.');return data;
}
function buttons() {
 $('test-buttons').innerHTML=Array.from({length:10},(_,i)=>`<button class="test-button ${selected===i+1?'active':''} ${completed.has(i+1)?'completed':''}" data-test="${i+1}" title="${escape(titles[i+1])}" aria-label="Select Test ${i+1}: ${escape(titles[i+1])}" aria-pressed="${selected===i+1}"><span>${String(i+1).padStart(2,'0')}</span>Test ${i+1}</button>`).join('');
}
function codeRange() {
 const lines=source.split('\n');let start=0,end=lines.length;
 if(codeSection==='test') {
  start=lines.findIndex(l=>l.includes(`// TEST ${selected}:`));
  end=lines.findIndex((l,i)=>i>start && (l.includes('// TEST ') || l.includes('default:')));
  if(start<0)start=0;if(end<0)end=lines.length;
 } else if(codeSection==='comparator') {
  start=lines.findIndex(l=>l.includes('// COMPARATOR:'));end=lines.findIndex(l=>l.includes('// DATA STRUCTURES:'));
 } else if(codeSection==='structures') {
  start=lines.findIndex(l=>l.includes('// DATA STRUCTURES:'));end=lines.findIndex(l=>l.includes('// REGISTRATION:'));
 }
 return {lines,start,end};
}
function renderCode() {
 const {lines,start,end}=codeRange();
 const shown=lines.slice(start,end);
 const indentation=Math.min(...shown.filter(l=>l.trim()).map(l=>l.match(/^ */)[0].length));
 const selectedStart=lines.findIndex(l=>l.includes(`// TEST ${selected}:`));
 const selectedEnd=lines.findIndex((l,i)=>i>selectedStart && (l.includes('// TEST ')||l.includes('default:')));
 $('code').innerHTML=lines.slice(start,end).map((line,i)=>`<span class="code-line ${codeSection==='test'||(start+i>=selectedStart&&start+i<selectedEnd)?'highlight':''}"><span class="line-number">${start+i+1}</span><span class="${line.trim().startsWith('//')?'code-comment':''}">${escape(line.slice(Number.isFinite(indentation)?indentation:0))}</span></span>`).join('');
 $('code-caption').textContent=codeSection==='test'?`Test ${selected} · ${titles[selected]} — actual Java code` : codeSection==='all'?'Full source · comments explain every operation':codeSection==='comparator'?'Severity → arrival time → sequence: FIFO within each level':'The heap, the fixed room array, and the patient lookup map';
 document.querySelectorAll('[data-section]').forEach(b=>b.classList.toggle('active',b.dataset.section===codeSection));
}
function patientCard(p) {
 return `<div class="patient" data-level="${p.level}" style="--level:${levelColors[p.level]}"><div class="patient-id">${escape(p.id)}<span class="badge">${p.level}</span></div><div class="patient-name">${escape(p.name)}</div><div class="patient-time">#${p.sequence} · ${new Date(p.arrival).toLocaleTimeString('en-US',{hour:'2-digit',minute:'2-digit',timeZone:'UTC'})} UTC</div>${arrivalView?`<div class="status-tag">${escape(p.status)}</div>`:''}</div>`;
}
function renderState() {
 const state=showingBefore?beforeState:afterState;if(!state)return;
 $('waiting-count').textContent=state.waiting.length;$('occupied-count').textContent=state.occupied;
 $('available-count').textContent=10-state.occupied;$('discharged-count').textContent=state.discharged;
 const people=arrivalView?state.arrivals:state.waiting;
 $('queue-title').textContent=arrivalView?'All arrivals · includes treated patients':'Waiting queue';
 $('queue').innerHTML=people.length?people.map(patientCard).join(''):'<div class="empty-queue">No patients waiting.</div>';
 $('severity-counts').innerHTML=state.counts.map((count,i)=>`<span><strong style="color:${levelColors[i+1]}">ESI ${i+1}</strong> · ${count} waiting</span>`).join('');
 $('rooms').innerHTML=state.rooms.map((p,i)=>{
  const old=beforeState?.rooms[i];const changed=!!afterState && !showingBefore && ((old?.id||'')!==(p?.id||''));
  return `<div class="room ${p?'occupied':''} ${changed?'changed':''}" style="--level:${p?levelColors[p.level]:'#9baa93'}"><div class="room-top"><span>ROOM ${String(i+1).padStart(2,'0')}</span>${p?`<span class="badge">${p.level}</span>`:'<span>○</span>'}</div><p>${p?escape(p.name):'Available'}</p><small>${p?escape(p.id)+' · ESI '+p.level:'Ready for a patient'}</small></div>`;
 }).join('');
 $('before').classList.toggle('active',showingBefore);$('after').classList.toggle('active',!showingBefore);
 $('before').setAttribute('aria-pressed',showingBefore);$('after').setAttribute('aria-pressed',!showingBefore);
 $('priority-view').classList.toggle('active',!arrivalView);$('arrival-view').classList.toggle('active',arrivalView);
}
async function selectTest(n) {
 selected=n;const request=++activeRequest;showingBefore=true;beforeState=null;afterState=null;
 $('scenario-title').textContent=`${String(n).padStart(2,'0')} / ${titles[n]}`;
 $('explanation').textContent=descriptions[n];$('run').textContent=`▶ Run Test ${n}`;$('run').disabled=true;
 $('next').disabled=n===10;$('result').className='result';$('result').textContent='Preparing…';
 $('events').innerHTML='<p>Preparing the starting state in Java…</p>';$('log-label').textContent='Before this test';
 buttons();renderCode();notice('');
 try {
  const state=await api(`/api/prepare?test=${n}`,{method:'POST',headers:{'Content-Type':'text/csv'},body:currentCsv});if(request!==activeRequest)return;
  beforeState=state;afterState=state;renderState();$('result').textContent='Ready';
  $('events').innerHTML='<p>Starting state restored. Press Run to execute this test.</p>';$('run').disabled=false;
 } catch(e){if(request!==activeRequest)return;notice(e.message);$('result').textContent='Data needs attention';}
}
async function runTest() {
 const n=selected,request=++activeRequest;$('run').disabled=true;$('result').textContent='Running Java…';notice('');
 try {
  const result=await api(`/api/run?test=${n}`,{method:'POST',headers:{'Content-Type':'text/csv'},body:currentCsv});if(request!==activeRequest)return;
  beforeState=result.before;afterState=result.after;showingBefore=false;
  $('explanation').textContent=result.explanation;$('result').textContent=result.passed?'✓ Test passed':'Test needs attention';
  $('result').className='result '+(result.passed?'pass':'fail');
  $('events').innerHTML=result.log.map(line=>`<div class="event ${line.startsWith('REJECTED')?'reject':line.startsWith('ADMITTED')||line.startsWith('DISCHARGED')?'success':''}">${escape(line)}</div>`).join('') || '<p>No changes.</p>';
  $('log-label').textContent='Actual Java output';if(result.passed)completed.add(n);buttons();renderState();
 }catch(e){if(request===activeRequest)notice(e.message);}
 finally{if(request===activeRequest)$('run').disabled=false;}
}
$('test-buttons').addEventListener('click',e=>{const b=e.target.closest('[data-test]');if(b)selectTest(Number(b.dataset.test));});
$('run').onclick=runTest;$('next').onclick=()=>selectTest(Math.min(10,selected+1));
$('before').onclick=()=>{showingBefore=true;renderState();};$('after').onclick=()=>{showingBefore=false;renderState();};
$('arrival-view').onclick=()=>{arrivalView=true;renderState();};$('priority-view').onclick=()=>{arrivalView=false;renderState();};
document.querySelectorAll('[data-section]').forEach(b=>b.onclick=()=>{codeSection=b.dataset.section;renderCode();});
function download(blob,name) {
 const url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(url),10000);
}
$('download-code').onclick=()=>download(new Blob([source],{type:'text/plain'}),'EDProgram.java');
async function importCsv(csv) {
 await api('/api/import',{method:'POST',headers:{'Content-Type':'text/csv'},body:csv});currentCsv=csv;completed.clear();await selectTest(1);
}
$('csv').onchange=async e=>{
 const file=e.target.files[0];if(!file)return;
 try{await importCsv(await file.text());notice(`Imported ${file.name}. Custom data can change test results; the supplied sample is designed for all ten scenarios.`);}catch(err){notice(err.message);}finally{e.target.value='';}
};
$('sample').onclick=async()=>{try{await importCsv(await (await fetch('/api/sample')).text());notice('Sample data restored.');}catch(e){notice(e.message);}};
const requirements=[['PriorityQueue + custom PatientComparator','Comparator tab; Tests 1 and 4'],['Five ESI levels and assignment colours','Patient badges and severity legend'],['FIFO within the same priority','Arrival and treatment order; sample P001 before P006'],['Arrival-time tracking','Patient arrival times and sequence numbers'],['admitPatient(), getNextPatient(), peekNextPatient()','Tests 1 and 10; commented Java source'],['Monitoring, queue size and statistics','Counters, room grid, severity counts and Java output'],['Occupied rooms, full capacity and null input','Tests 3, 4 and 5'],['Discharge, updated rooms and empty-room rejection','Tests 6, 7, 8 and 9'],['Final queue/occupancy and search/peek','Test 10']];
$('requirements-list').innerHTML=requirements.map(([a,b])=>`<div class="requirement"><span>✓</span><strong>${a}</strong><br><small>${b}</small></div>`).join('');
$('requirements').onclick=()=>$('checklist').showModal();$('close-checklist').onclick=()=>$('checklist').close();
// RECORDING: user chooses the screen and microphone using the browser's prompts.
// No automatic capture on page load, no uploads, no third-party recording service.
let recorder,streams=[],chunks=[],timer,started,videoUrl;
$('record').onclick=()=>{if(recorder?.state==='recording'){notice('A recording is already running. Use Stop & save.');return;}$('recording-dialog').showModal();};
$('close-recording').onclick=()=>$('recording-dialog').close();
function cleanupRecording() {clearInterval(timer);streams.forEach(s=>s.getTracks().forEach(t=>t.stop()));streams=[];$('recording-bar').hidden=true;$('record').disabled=false;}
$('start-recording').onclick=async()=>{
 $('start-recording').disabled=true;notice('');
 try {
  if(!navigator.mediaDevices?.getDisplayMedia || !window.MediaRecorder)throw Error('This browser does not support screen recording. Use Chrome, or Mac Shift–Command–5.');
  const screen=await navigator.mediaDevices.getDisplayMedia({video:{frameRate:30},audio:false});streams.push(screen);
  let tracks=[...screen.getVideoTracks()];
  if($('use-mic').checked){const mic=await navigator.mediaDevices.getUserMedia({audio:true});streams.push(mic);tracks.push(...mic.getAudioTracks());}
  const combined=new MediaStream(tracks);
  const mime=['video/webm;codecs=vp9,opus','video/webm;codecs=vp8,opus','video/mp4','video/webm'].find(t=>MediaRecorder.isTypeSupported(t));
  recorder=new MediaRecorder(combined,mime?{mimeType:mime}:{});chunks=[];
  recorder.ondataavailable=e=>{if(e.data.size)chunks.push(e.data);};
  recorder.onstop=()=>{
   const blob=new Blob(chunks,{type:recorder.mimeType});if(videoUrl)URL.revokeObjectURL(videoUrl);videoUrl=URL.createObjectURL(blob);
   $('video-download').href=videoUrl;$('video-download').download='DS_in_ED_Walkthrough.'+(recorder.mimeType.includes('mp4')?'mp4':'webm');
   $('saved-recording').hidden=false;cleanupRecording();
  };
  recorder.onerror=()=>{notice('Recording failed. Try Mac Shift–Command–5.');cleanupRecording();};
  screen.getVideoTracks()[0].onended=()=>{if(recorder?.state==='recording')recorder.stop();};
  recorder.start(1000);started=Date.now();$('recording-time').textContent='00:00';$('recording-bar').hidden=false;$('record').disabled=true;
  timer=setInterval(()=>{const t=Math.floor((Date.now()-started)/1000);$('recording-time').textContent=String(Math.floor(t/60)).padStart(2,'0')+':'+String(t%60).padStart(2,'0');},1000);
  $('recording-dialog').close();
 }catch(e){cleanupRecording();notice(e.name==='NotAllowedError'?'Recording permission was declined. Try again when ready, or use Mac Shift–Command–5.':e.message);}
 finally{$('start-recording').disabled=false;}
};
$('stop-recording').onclick=()=>{if(recorder?.state==='recording')recorder.stop();};
$('dismiss-video').onclick=()=>{$('saved-recording').hidden=true;};
window.addEventListener('beforeunload',e=>{if(recorder?.state==='recording'||videoUrl){e.preventDefault();e.returnValue='Download your recording before leaving.';}});
(async()=>{
 try{currentCsv=await (await fetch('/api/sample')).text();const response=await fetch('/api/source');if(!response.ok)throw Error('Java source could not be loaded.');source=await response.text();$('connection').textContent='Java is running';await selectTest(1);}
 catch(e){$('connection').textContent='Java offline';notice('The Java server is unavailable. Open Start-Website.command and reload. '+e.message);}
})();
