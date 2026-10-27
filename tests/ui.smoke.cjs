// npm install --prefix /tmp/skyncar-test playwright
// NODE_PATH=/tmp/skyncar-test/node_modules node tests/ui.smoke.cjs
const {chromium}=require('playwright');
const http=require('node:http');const fs=require('node:fs');const path=require('node:path');const assert=require('node:assert/strict');
(async()=>{
 const root=path.resolve(__dirname,'../assets');
 const server=http.createServer((req,res)=>{const file=path.resolve(root,'.'+decodeURIComponent(req.url==='/'?'/index.html':req.url));if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return;}fs.readFile(file,(e,b)=>{res.writeHead(e?404:200,{'Content-Type':file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.png')?'image/png':'text/html'});res.end(e?'not found':b);});});
 await new Promise(r=>server.listen(0,'127.0.0.1',r));
 let browser;try{
 browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_PATH||'/usr/bin/chromium',args:['--no-sandbox']});const page=await browser.newPage({viewport:{width:1024,height:600}});const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.goto('http://127.0.0.1:'+server.address().port);await page.locator('h1').filter({hasText:'Hola, Isias'}).waitFor();
 for(const id of ['navigation','music','phone','vehicle','split','apps','radio','settings','home']){await page.locator(`[data-page="${id}"]`).click();assert(await page.locator('#screen').innerText());}
 await page.locator('[data-page="settings"]').click();await page.locator('[name="name"]').fill('Isias Romero');await page.locator('[name="theme"]').selectOption('light');await page.getByText('Guardar preferencias',{exact:true}).click();assert.equal(await page.locator('html').getAttribute('data-theme'),'light');
 await page.reload();await page.locator('h1').filter({hasText:'Hola, Isias Romero'}).waitFor();
 await page.locator('[data-page="vehicle"]').click();await page.locator('[name="current"]').fill('12000');await page.locator('[name="last"]').fill('10000');await page.locator('[name="interval"]').fill('5000');await page.getByText('Guardar mantenimiento',{exact:true}).click();assert.match(await page.locator('#screen').innerText(),/3[.,]000 km/);
 await page.reload();await page.locator('[data-page="vehicle"]').click();assert.equal(await page.locator('[name="current"]').inputValue(),'12000');
 await page.locator('[data-page="home"]').click();await page.locator('#sleep').click();assert(await page.locator('#rest').isVisible());await page.locator('#wake').click();assert(!(await page.locator('#rest').isVisible()));
 for(const viewport of [{width:1024,height:600},{width:800,height:480},{width:1280,height:720}]){await page.setViewportSize(viewport);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));}
 await page.setViewportSize({width:1024,height:600});await page.locator('[data-page="settings"]').click();await page.locator('[name="name"]').fill('Isias');await page.locator('[name="theme"]').selectOption('dark');await page.getByText('Guardar preferencias',{exact:true}).click();await page.locator('[data-page="home"]').click();
 const output=path.resolve(__dirname,'../build');fs.mkdirSync(output,{recursive:true});await page.screenshot({path:path.join(output,'home.png')});assert.deepEqual(errors,[]);console.log('UI: nine screens, profile and oil persistence, themes, rest and 3 sizes passed.');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exitCode=1;});
