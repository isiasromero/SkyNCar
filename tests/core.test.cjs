const {test}=require('node:test');
const assert=require('node:assert/strict');
const {greeting,remaining,validOil}=require('../assets/core.js');
test('saludos cambian en los límites horarios y medianoche',()=>{
 for(const h of [0,4,19,23])assert.match(greeting(h),/noche/);
 for(const h of [5,11])assert.match(greeting(h),/mañana/);
 for(const h of [12,18])assert.match(greeting(h),/tarde/);
});
test('mantenimiento distingue pendiente, vencido y próximo',()=>{
 assert.equal(remaining(12000,10000,5000),3000);
 assert.equal(remaining(15000,10000,5000),0);
 assert.equal(remaining(16000,10000,5000),-1000);
});
test('no permite intervalos inválidos o retroceder el odómetro',()=>{
 assert.equal(validOil('12000','10000','5000'),true);
 for(const a of [['9','10','5000'],['10','10','0'],['10','10','abc'],['','0','5000'],['10','-1','5000']])assert.equal(validOil(...a),false);
});
