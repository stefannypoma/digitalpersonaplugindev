package pt.deloitte.entel.plugin;

public interface DPManagerCallback {
    void onDPFingerStatusUpdate(int DPFingerStatus);

    void onBitmapUpdate(int width, int height, String base64String);

    void onPercentageUpdate(int percentage);

    void onDPStatusUpdate(int DPStatus);

    void onError(int errorCode);

    void onSDKError(int sdkErrorCode, String errorMessage);
}
