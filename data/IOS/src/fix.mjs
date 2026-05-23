import fs from 'fs';
let c = fs.readFileSync('App.css', 'utf8');
const idx = c.indexOf('.hide-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }');
if(idx !== -1) {
  fs.writeFileSync('App.css', c.substring(0, idx + 69) + '\n.kat-switch { position: relative; display: inline-block; width: 50px; height: 28px; }\n.kat-switch input { opacity: 0; width: 0; height: 0; }\n.kat-slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: var(--border); transition: .4s; border-radius: 34px; }\n.kat-slider:before { position: absolute; content: ""; height: 20px; width: 20px; left: 4px; bottom: 4px; background-color: white; transition: .4s; border-radius: 50%; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\ninput:checked + .kat-slider { background-color: var(--positive); }\ninput:checked + .kat-slider:before { transform: translateX(22px); }\n');
}
