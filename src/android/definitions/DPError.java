package pt.deloitte.entel.plugin.definitions;

public @interface DPError {
    int UNEXPECTED = -1;
    int PERMISSION_DENIED = 1;
    int NO_DEVICE_FOUND = 2;
    int INVALID_CONNECTION = 3;
}