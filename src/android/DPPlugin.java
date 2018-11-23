package pt.deloitte.entel.plugin;

import android.content.Context;
import android.widget.Toast;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pt.deloitte.entel.plugin.DPManager;
import pt.deloitte.entel.plugin.DPManagerCallback;

public class DPPlugin extends CordovaPlugin {
    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) {
        if (action.equals("start")) {
            
			Context context = cordova.getActivity().getApplicationContext();
            DPManager.getInstance().initialize(context, getDPManagerCallback(context, callbackContext));

            PluginResult pluginResult = new  PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        }

        if (action.equals("stop")) {
            DPManager.getInstance().stop();
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        }
		
		if (action.equals("connect")){
			Context context = cordova.getActivity().getApplicationContext();
            DPManager.getInstance().initialize2(context, getDPManagerCallback(context, callbackContext));
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        }
		

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        callbackContext.sendPluginResult(pluginResult);
        return true;
    }

    private DPManagerCallback getDPManagerCallback(Context context, final CallbackContext callbackContext) {
        return new DPManagerCallback() {
            public void onDPFingerStatusUpdate(int DPFingerStatus) {
                PluginResult pluginResult = new  PluginResult(PluginResult.Status.OK, "{\"onDPFingerStatusUpdate\":{\"DPFingerStatus\":"+DPFingerStatus+"}}");
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            }

            public void onBitmapUpdate(int width, int height, String base64String) {
                PluginResult pluginResult = new  PluginResult(PluginResult.Status.OK, "{\"onBitmapUpdate\":{\"width\":"+width+",\"height\":"+height+",\"base64String\":\""+base64String+"\"}}");
                pluginResult.setKeepCallback(true);
				Log.d("FingerBase64",base64String);
                callbackContext.sendPluginResult(pluginResult);
            }

            public void onPercentageUpdate(int percentage) {
                PluginResult pluginResult = new  PluginResult(PluginResult.Status.OK, "{\"onPercentageUpdate\":{\"percentage\":"+percentage+"}}");
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            }

            public void onDPStatusUpdate(int DPStatus) {
                PluginResult pluginResult = new  PluginResult(PluginResult.Status.OK, "{\"onDPStatusUpdate\":{\"DPStatus\":"+DPStatus+"}}");
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            }

            public void onError(int errorCode) {
                PluginResult pluginResult = new  PluginResult(PluginResult.Status.ERROR, "{\"onError\":{\"errorCode\":"+errorCode+"}}");
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            }

            public void onSDKError(int sdkErrorCode, String errorMessage) {
                PluginResult pluginResult = new  PluginResult(PluginResult.Status.ERROR, "{\"onSDKError\":{\"sdkErrorCode\":"+sdkErrorCode+",\"errorMessage\":\""+errorMessage+"\"}}");
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            }
        };
    }
}
