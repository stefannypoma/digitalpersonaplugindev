package pt.deloitte.entel.plugin;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Base64;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.os.Environment;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.Reader.Priority;
import com.digitalpersona.uareu.Fid;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.ReaderCollection;
import com.digitalpersona.uareu.UareUGlobal;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbHost;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbException;
import com.digitalpersona.uareu.Compression.CompressionAlgorithm;
import com.digitalpersona.uareu.dpfj.CompressionImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import pt.deloitte.entel.plugin.definitions.DPError;
import pt.deloitte.entel.plugin.definitions.DPMessageType;
import pt.deloitte.entel.plugin.definitions.DPStatus;

public class DPManager {
    
	private static ReaderCollection m_collection;
	
	private static final String ACTION_USB_PERMISSION = "com.digitalpersona.uareu.dpfpddusbhost.USB_PERMISSION";
	
    private static DPManager DPManager;

    public static synchronized DPManager getInstance() {
        if (DPManager == null)
            DPManager = new DPManager();
        return DPManager;
    }

	private Reader m_reader;
    private Context context;
    private DPManagerCallback DPManagerCallback;
    
	private String m_readerName;
	private boolean m_reset = false;
	
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
                            CheckEikonDevice();
                        }
                    }
                }
            }
        }
    };

    public void initialize(Context newContext, DPManagerCallback newDPManagerCallback) {
        context = newContext;
        DPManagerCallback = newDPManagerCallback;
		
		try {
			m_collection = UareUGlobal.GetReaderCollection(context);
			m_collection.GetReaders();
			m_reader = m_collection.get(0);
			m_readerName = m_reader.GetDescription().name;
			IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
			context.registerReceiver(mUsbReceiver, filter);
            if (DPFPDDUsbHost.DPFPDDUsbCheckAndRequestPermissions(context, PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0), m_readerName)) {
				UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
				HashMap<String, UsbDevice> usbDeviceHashMap = usbManager.getDeviceList();
				if (usbDeviceHashMap.isEmpty()) {
					DPManagerCallback.onDPStatusUpdate(DPStatus.STOPED);
					DPManagerCallback.onError(DPError.NO_DEVICE_FOUND);
				} else {
					UsbDevice usbDevice = usbDeviceHashMap.values().iterator().next();
					if(usbDevice.getVendorId() != 5246) return;
					if (usbManager.hasPermission(usbDevice)) {
						DPManagerCallback.onDPStatusUpdate(DPStatus.STARTED);
						CheckEikonDevice();
						readEikonDevice();
					} else {
						usbManager.requestPermission(usbDevice, PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0));
					}
				}
            }
		} catch (Exception e1) {
			DPManagerCallback.onDPStatusUpdate(DPStatus.STOPED);
			DPManagerCallback.onError(DPError.UNEXPECTED);
		}
    }
	
	public void initialize2(Context newContext, DPManagerCallback newDPManagerCallback) {
        context = newContext;
        DPManagerCallback = newDPManagerCallback;
		
		try {
			m_collection = UareUGlobal.GetReaderCollection(context);
			m_collection.GetReaders();
			m_reader = m_collection.get(0);
			m_readerName = m_reader.GetDescription().name;
			IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
			context.registerReceiver(mUsbReceiver, filter);
            if (DPFPDDUsbHost.DPFPDDUsbCheckAndRequestPermissions(context, PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0), m_readerName)) {
				UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
				HashMap<String, UsbDevice> usbDeviceHashMap = usbManager.getDeviceList();
				if (usbDeviceHashMap.isEmpty()) {
					DPManagerCallback.onDPStatusUpdate(DPStatus.DISCONNECTED);
					DPManagerCallback.onError(DPError.NO_DEVICE_FOUND);
				} else {
					UsbDevice usbDevice = usbDeviceHashMap.values().iterator().next();
					if(usbDevice.getVendorId() != 5246) return;
					if (usbManager.hasPermission(usbDevice)) {
						CheckEikonDevice();
						DPManagerCallback.onDPStatusUpdate(DPStatus.CONNECTED);
					} else {
						usbManager.requestPermission(usbDevice, PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0));
						DPManagerCallback.onDPStatusUpdate(DPStatus.CONNECTED);
					}
				}
            } else {
				DPManagerCallback.onDPStatusUpdate(DPStatus.DISCONNECTED);
			}
		} catch (Exception e1) {
			DPManagerCallback.onDPStatusUpdate(DPStatus.DISCONNECTED);
		}
    }
	
	private void CheckEikonDevice()
	{
		try {
			m_reader.Open(Priority.EXCLUSIVE);
		} 
		catch (UareUException e){
			Log.d("DPManager", m_reader.GetDescription().technology + ": " + m_readerName + ": " + e.getMessage());
		}
		catch (Exception e1)
		{
			Log.d("DPManager", m_reader.GetDescription().technology + ": " + m_readerName + ": " + e1.getMessage());
		}
	}
	
	private void readEikonDevice() {
		new Thread(new Runnable()
		{
			public void run()
			{
				try 
				{
					m_reset = false;
					while (!m_reset)
					{
						DPManagerCallback.onDPStatusUpdate(DPStatus.SCANNING);
						Log.d("DPManager","Capturing...");
						Reader.CaptureResult cap_result = m_reader.Capture(Fid.Format.ANSI_381_2004, Reader.ImageProcessing.IMG_PROC_DEFAULT, 500, -1);
						if(cap_result != null){
							Log.d("DPManager","Captured not null");
							Fid ISOFid = cap_result.image;
							byte[] rawCompress = processImage(ISOFid.getViews()[0].getData(),ISOFid.getViews()[0].getWidth(), ISOFid.getViews()[0].getHeight());
							
							String base64 = encode(rawCompress);
							Log.d("DPManager",base64);
							
							DPManagerCallback.onBitmapUpdate(0, 0, base64);
							
							Log.d("DPManager",base64);
							
							if(base64 != "") stop();
						} else {
							DPManagerCallback.onDPStatusUpdate(DPStatus.STOPED);
							DPManagerCallback.onError(DPError.UNEXPECTED);
						}
					}
				}catch (Exception e)
				{	
					DPManagerCallback.onDPStatusUpdate(DPStatus.STOPED);
					DPManagerCallback.onError(DPError.UNEXPECTED);
				}
			}
		}).start();
	}
	
	public void stop() {
		try 
		{
			m_reset = true;
			try {m_reader.CancelCapture(); } catch (Exception e) {}
			m_reader.Close();
			DPManagerCallback.onDPStatusUpdate(DPStatus.STOPED);
		}
		catch (Exception e)
		{
			DPManagerCallback.onDPStatusUpdate(DPStatus.STOPED);
            DPManagerCallback.onError(DPError.UNEXPECTED);
		}
    }
	
	private String encode(byte[] d) {
        if (d == null) {
            return null;
        }
        int idx;
        byte[] data = new byte[(d.length + 2)];
        System.arraycopy(d, 0, data, 0, d.length);
        byte[] dest = new byte[((data.length / 3) * 4)];
        int sidx = 0;
        int didx = 0;
        while (sidx < d.length) {
            dest[didx] = (byte) ((data[sidx] >>> 2) & 63);
            dest[didx + 1] = (byte) (((data[sidx + 1] >>> 4) & 15) | ((data[sidx] << 4) & 63));
            dest[didx + 2] = (byte) (((data[sidx + 2] >>> 6) & 3) | ((data[sidx + 1] << 2) & 63));
            dest[didx + 3] = (byte) (data[sidx + 2] & 63);
            sidx += 3;
            didx += 4;
        }
        for (idx = 0; idx < dest.length; idx++) {
            if (dest[idx] < (byte) 26) {
                dest[idx] = (byte) (dest[idx] + 65);
            } else if (dest[idx] < (byte) 52) {
                dest[idx] = (byte) ((dest[idx] + 97) - 26);
            } else if (dest[idx] < (byte) 62) {
                dest[idx] = (byte) ((dest[idx] + 48) - 52);
            } else if (dest[idx] < (byte) 63) {
                dest[idx] = (byte) 43;
            } else {
                dest[idx] = (byte) 47;
            }
        }
        for (idx = dest.length - 1; idx > (d.length * 4) / 3; idx--) {
            dest[idx] = (byte) 61;
        }
        return new String(dest);
    }
	
	public byte[] processImage(byte[] img, int width, int height){
            
            
            
             Bitmap bmWSQ = null;
             bmWSQ = getBitmapAlpha8FromRaw(img, width,
                           height);
 
             byte[] arrayT = null;
 
             Bitmap redimWSQ = overlay(bmWSQ);
             int numOfbytes = redimWSQ.getByteCount();
             ByteBuffer buffer = ByteBuffer.allocate(numOfbytes);
             redimWSQ.copyPixelsToBuffer(buffer);
             arrayT = buffer.array();
 
             int v1 = 1;
             for (int i = 0; i < arrayT.length; i++) {
                    if (i < 40448) { // 79
                           arrayT[i] = (byte) 255;
                    } else if (i >= 40448 && i <= 221696) {
 
                           if (v1 < 132) {
                                  arrayT[i] = (byte) 255;
                           } else if (v1 > 382) {
                                  arrayT[i] = (byte) 255;
                           }
                           if (v1 == 512) {
                                  v1 = 0;
                           }
                           v1++;
                    } else if (i > 221696) { // 433
                           arrayT[i] = (byte) 255;
                    }
 
             }
 
             CompressionImpl comp = new CompressionImpl();
             try {
                    comp.Start();
                    comp.SetWsqBitrate(500, 0);
					
                    byte[] rawCompress = comp.CompressRaw(arrayT, redimWSQ.getWidth(), redimWSQ.getHeight(), 500, 8,
                                  CompressionAlgorithm.COMPRESSION_WSQ_NIST);
                    
                    comp.Finish();
                   
                    Log.i("Util", "getting WSQ...");
 
                    return rawCompress;
                   
             } catch (UareUException e) {
                    Log.e("Util", "UareUException..." + e);
                    return null;
             } catch (Exception e) {
                    Log.e("Util", "Exception..." + e);
                    return null;
             }
 
      
            
       }
	   
	   private Bitmap overlay(Bitmap bmp) {
             Bitmap bmOverlay = Bitmap.createBitmap(512, 512, Config.ALPHA_8);
             Canvas canvas = new Canvas(bmOverlay);
             canvas.drawBitmap(bmp, 512 / 2 - bmp.getWidth() / 2, 512 / 2 - bmp.getHeight() / 2, null);
             canvas.save();
             return bmOverlay;
       }
	   
	   private Bitmap getBitmapAlpha8FromRaw(byte[] Src, int width, int height)
	   { 
             byte [] Bits = new byte[Src.length];
             int i = 0;
             for(i=0;i<Src.length;i++)
             {
                    Bits[i] = Src[i];
             }
            
             Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
             bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
            
             return bitmap;
       }
    //endregion

}