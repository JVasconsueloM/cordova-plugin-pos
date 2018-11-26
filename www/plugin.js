
var exec = require('cordova/exec');

var PLUGIN_NAME = 'POSPlugin';

var POSPlugin = {
  	print: function (successCallback, errorCallback, content, options){
  		options = options || {};
  		content = content || [];
        exec(successCallback, errorCallback, PLUGIN_NAME, 'PRINT', [content, options]);
  	},
  	getStatus: function (successCallback, errorCallback){
        exec(successCallback, errorCallback, PLUGIN_NAME, 'STATUS_PRINTER', []);
  	}
};

module.exports = POSPlugin;
