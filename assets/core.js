(function(root){
'use strict';
function greeting(hour){if(hour>=5&&hour<12)return '¡Que tengas una buena mañana!';if(hour>=12&&hour<19)return '¡Que tengas una buena tarde!';return '¡Que tengas una buena noche!';}
function remaining(current,last,interval){return Number(last)+Number(interval)-Number(current);}
function validOil(current,last,interval){return [current,last,interval].every(v=>String(v).trim()!==''&&Number.isFinite(Number(v)))&&Number(current)>=Number(last)&&Number(last)>=0&&Number(interval)>0;}
const api={greeting,remaining,validOil};root.SkyCore=api;if(typeof module!=='undefined')module.exports=api;
})(typeof window!=='undefined'?window:globalThis);
