package pt.deloitte.entel.plugin.definitions;

public @interface DPStatus {
    int DISCONNECTED = -1;
	int CONNECTED = 0;
	int STARTED = 1;
    int SCANNING = 2;
    int STOPED = 3;
}