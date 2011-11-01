package nl.michiel1972.main;


import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.security.auth.login.LoginException;

import nl.michiel1972.main.R;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.nfc.FormatException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class UploadToCommonsActivity extends Activity {
	
	
	private String username="";
	private String password="";
	private String filename="";
	private String detailinfo="";
	private String licenseinfo="";
	private String exif_datetime="";
	private Wiki theWiki;
	private byte[] data;
	private boolean doOverwrite=false;


	/* Called when the activity is first created. This is shown only once with empty parameters 
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.main); 

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		String action = intent.getAction();
 
		
		
		// if this is from the share menu
		if (Intent.ACTION_SEND.equals(action))
		{
			if (extras.containsKey(Intent.EXTRA_STREAM))
			{
				try
				{
					
					
					// Hide standard keyboard
					getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
					
					// Previously stored settings if any
					LoadPreferences();
					
					// Get resource path from intent
					Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);

					// Query gallery for camera picture via
					// Android ContentResolver interface
					ContentResolver cr = getContentResolver();
					InputStream is = cr.openInputStream(uri);
					
					// Get binary bytes for encode of image file
					data = getBytesFromFile(is);
					
					// Get Exif date, convert into correct format
					String imagefile = getRealPathFromURI(uri);
				    ExifInterface exifInterface = new ExifInterface(imagefile);
					exif_datetime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
					
					SimpleDateFormat dateParser = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
					SimpleDateFormat dateConverter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					Date d = dateParser.parse(exif_datetime);
					exif_datetime = dateConverter.format(d);
					
					// Suggest image filename using original filename plus username prefix
					EditText temp = (EditText) this.findViewById(R.id.editText1);
					username = temp.getText().toString().trim();
					String filename_suggestion =  "Img_by_"+username+"_"+uri.getLastPathSegment()+".jpg";
					temp = (EditText) this.findViewById(R.id.editText3);
					temp.setText(filename_suggestion);
					
					// Suggest description
					//temp = (EditText) this.findViewById(R.id.editText4);
					//temp.setText("");
					
					// Suggest license template - to be edited further if wanted by user
					temp = (EditText) this.findViewById(R.id.editText5);
					temp.setText("{{self|cc-by-sa-3.0|GFDL}}");

					// BUTTON CLICK
					final Button button = (Button) findViewById(R.id.button1);
			         button.setOnClickListener(new View.OnClickListener() {
			             public void onClick(View v) {
			                 // Perform action on click
			            	 button.setClickable(false);
			            	 button.setText("Busy uploading..please wait");
			            	 try {
								startProcessingUpload();
							} catch (LoginException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
			             }


			         });
			         
					return;
				} catch (Exception e)
				{
					Log.e(this.getClass().getName(), e.toString());
				}

			} else if (extras.containsKey(Intent.EXTRA_TEXT))
			{
				return;
			}
		}

	}

    /**
     * Get real path from Uri
     * @param contentUri
     * @return
     */
    
    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
        

    /**
     * Returns the contents of the file in a byte array
     * @param is
     * @return
     * @throws IOException
     */
    public static byte[] getBytesFromFile(InputStream is) throws IOException {
    
        // Get the size of the file
        long length = is.available();

        // You cannot create an array using a long type.
        // It needs to be an int type.
        // Before converting to an int type, check
        // to ensure that file is not larger than Integer.MAX_VALUE.
        if (length > Integer.MAX_VALUE) {
            // File is too large
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
               && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file ");
        }

        // Close the input stream and return bytes
        is.close();
        return bytes;
    }
    
    /**
     * Save settings locally
     * @param key
     * @param value
     */
    private void SavePreferences(String key, String value){
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
       }
    
    /**
     * Load settings locally
     */
    private void LoadPreferences(){
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        String strSavedMem1 = sharedPreferences.getString("MEM1", "unknown");
        String strSavedMem2 = sharedPreferences.getString("MEM2", "unknown");
        String strSavedMem5 = sharedPreferences.getString("MEM5", "unknown");
        //Give found preferences if username was defined
        if (!strSavedMem1.equals("unknown")){
          EditText temp = (EditText) this.findViewById(R.id.editText1);
          temp.setText(strSavedMem1);
          temp = (EditText) this.findViewById(R.id.editText2);
          temp.setText(strSavedMem2);
          temp = (EditText) this.findViewById(R.id.editText5);
          temp.setText(strSavedMem5);
         }
       }
    
    
    /**
     * Start uploading
     * @throws LoginException 
     */
	protected void startProcessingUpload() throws LoginException {
		
		theWiki= new Wiki(this);
				

		
        //Get user data (data may be edited after suggestions)
		EditText temp = (EditText) this.findViewById(R.id.editText1);
		username = temp.getText().toString().trim();
		temp = (EditText) this.findViewById(R.id.editText2);
		password = temp.getText().toString().trim();
		Button button = (Button) findViewById(R.id.button1);
		temp = (EditText) this.findViewById(R.id.editText3);
		filename = temp.getText().toString().trim();
		temp = (EditText) this.findViewById(R.id.editText4);
		detailinfo = temp.getText().toString().trim();		
		temp = (EditText) this.findViewById(R.id.editText5);
		licenseinfo = temp.getText().toString().trim();
		
		//check if user id settings are changed
		//button click implies user thinks text fields are correctly updated
		SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        String strSavedMem1 = sharedPreferences.getString("MEM1", "unknown");
		if (!username.equals(strSavedMem1)){ 
			 Toast.makeText(this,"Login data stored", Toast.LENGTH_SHORT).show();
		         
		     SavePreferences("MEM1", username);
		     SavePreferences("MEM2", password);
		     SavePreferences("MEM5", licenseinfo);
		}
		
		//Try Login
		try {
			theWiki.login(username, password);
			Log.i(this.getClass().getName(), "login ok");
				
			//Contents description
			
			String description= 
					"==Summary==\n" +
				  "{{Information\n" +
					"|Description= " + detailinfo +
					"|Source= {{own}}\n" + 
					"|Date= " + exif_datetime + "\n" +
					"|Author= [[User:" +username +"|"+ username + "]]\n" +
					"|Permission= see below\n" +
					"|other_versions=\n" +
					"}}\n" +
					"{{Images uploaded with Android}}\n" +
					"==Licensing==\n" +
					licenseinfo +"\n"
					;
			
			//upload file bytes
			theWiki.uploadAndroid(data, 
                    filename, 
                    description, 
                    "");
			

			Handler handler = new Handler(); 
		    handler.postDelayed(new Runnable() { 
		         public void run() { 
		        	 finish(); 
		         } 
		    }, 2000); 
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(this.getClass().getName(), e.toString());
		} catch (FormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(this.getClass().getName(), e.toString());
		}
	       
		
	}

	 
	/**
	 * @return the doOverwrite
	 */
	public boolean isDoOverwrite() {
		return doOverwrite;
	}

	/**
	 * @param doOverwrite the doOverwrite to set
	 */
	public void setDoOverwrite(boolean doOverwrite) {
		this.doOverwrite = doOverwrite;
	}
	

}