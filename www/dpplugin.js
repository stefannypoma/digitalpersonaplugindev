// Empty constructor
function DPPlugin() {}

// The function that passes work along to native shells
// Message is a string, duration may be 'long' or 'short'
DPPlugin.prototype.start = function(successCallback, errorCallback) {
  var options = {};
  cordova.exec(successCallback, errorCallback, 'DPPlugin', 'start', [options]);
}

DPPlugin.prototype.connect = function(successCallback, errorCallback) {
  var options = {};
  cordova.exec(successCallback, errorCallback, 'DPPlugin', 'connect', [options]);
}

DPPlugin.prototype.stop = function(successCallback, errorCallback) {
  var options = {};
  cordova.exec(successCallback, errorCallback, 'DPPlugin', 'stop', [options]);
}

// Installation constructor that binds EntelPlugin to window
DPPlugin.install = function() {
  if (!window.plugins) {
    window.plugins = {};
  }
  window.plugins.dpPlugin = new DPPlugin();
  return window.plugins.dpPlugin;
};
cordova.addConstructor(DPPlugin.install);
