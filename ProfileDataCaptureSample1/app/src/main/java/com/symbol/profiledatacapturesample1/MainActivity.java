/*
* Copyright (C) 2015-2017 Zebra Technologies Corp
* All rights reserved.
*/
package com.symbol.profiledatacapturesample1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

public class MainActivity extends Activity {

    // Assign the profile name used in EMDKConfig.xml
    private String profileName = "DataCaptureProfile-1";

    TextView statusTextView = null;
    CheckBox checkBoxCode128 = null;
    CheckBox checkBoxCode39 = null;
    CheckBox checkBoxEAN8 = null;
    CheckBox checkBoxEAN13 = null;
    CheckBox checkBoxUPCA = null;
    CheckBox checkBoxUPCE0 = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = (TextView) findViewById(R.id.textViewStatus);
        checkBoxCode128 = (CheckBox) findViewById(R.id.checkBoxCode128);
        checkBoxCode39 = (CheckBox) findViewById(R.id.checkBoxCode39);
        checkBoxEAN8 = (CheckBox) findViewById(R.id.checkBoxEAN8);
        checkBoxEAN13 = (CheckBox) findViewById(R.id.checkBoxEAN13);
        checkBoxUPCA = (CheckBox) findViewById(R.id.checkBoxUPCE);
        checkBoxUPCE0 = (CheckBox) findViewById(R.id.checkBoxUPCE0);

        // Set listener to the button
        addSetButtonListener();

        //  Set initial profile
        modifyProfile_XMLString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.symbol.datawedge.api.RESULT_ACTION");
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(myBroadcastReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(myBroadcastReceiver);
    }

    private void addSetButtonListener()
    {
        Button buttonSet = (Button)findViewById(R.id.buttonSet);
        buttonSet.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                modifyProfile_XMLString();
            }
        });
    }

	private void modifyProfile_XMLString()
	{
		statusTextView.setText("");

		//  Tested and working with DataWedge 6.6.  Unlikely to work with earlier versions.

        //  1. Create the profile if it does not exist
        Bundle profileConfig = new Bundle();
        profileConfig.putString("PROFILE_NAME", profileName);
        profileConfig.putString("PROFILE_ENABLED", "true");
        profileConfig.putString("CONFIG_MODE", "CREATE_IF_NOT_EXIST");
        Bundle appConfig = new Bundle();
        appConfig.putString("PACKAGE_NAME", getPackageName());
        appConfig.putStringArray("ACTIVITY_LIST", new String[]{getPackageName() + ".MainActivity"});
        profileConfig.putParcelableArray("APP_LIST", new Bundle[]{appConfig});

        Intent dwCreateIntent = new Intent();
        dwCreateIntent.setAction("com.symbol.datawedge.api.ACTION");
        dwCreateIntent.putExtra("com.symbol.datawedge.api.SET_CONFIG", profileConfig);
        dwCreateIntent.putExtra("SEND_RESULT", "true");  //  Get feedback from DataWedge
        sendBroadcast(dwCreateIntent);

        //  2. Set the Barcode Input plugin
        profileConfig.remove("APP_LIST");
        profileConfig.remove("CONFIG_MODE");
        profileConfig.putString("CONFIG_MODE", "UPDATE");
        Bundle barcodeConfig = new Bundle();
        barcodeConfig.putString("PLUGIN_NAME", "BARCODE");
        barcodeConfig.putString("RESET_CONFIG", "true");
        Bundle barcodeProps = new Bundle();
        //  Can either use scanner_selection here or scanner_selection_by_identifier
        barcodeProps.putString("scanner_selection", "auto");   //  Requires DW 6.4
        //barcodeProps.putString("scanner_selection_by_identifier", "INTERNAL_IMAGER");   //  Requires DW 6.5
        barcodeProps.putString("scanner_input_enabled", "true");
        barcodeProps.putString("decoder_code128", String.valueOf(checkBoxCode128.isChecked()).toLowerCase());
        barcodeProps.putString("decoder_code39", String.valueOf(checkBoxCode39.isChecked()).toLowerCase());
        barcodeProps.putString("decoder_ean8", String.valueOf(checkBoxEAN8.isChecked()).toLowerCase());
        barcodeProps.putString("decoder_ean13", String.valueOf(checkBoxEAN13.isChecked()).toLowerCase());
        barcodeProps.putString("decoder_upca", String.valueOf(checkBoxUPCA.isChecked()).toLowerCase());
        barcodeProps.putString("decoder_upce0", String.valueOf(checkBoxUPCE0.isChecked()).toLowerCase());
        barcodeConfig.putBundle("PARAM_LIST", barcodeProps);
        //  Note: DW 6.6 supports the ability to define multiple plugin configs in the same intent but we
        //  separate it into multiple calls to be compatible with earlier versions, from 6.4
        profileConfig.putBundle("PLUGIN_CONFIG", barcodeConfig);

        Intent dwInputIntent = new Intent();
        dwInputIntent.setAction("com.symbol.datawedge.api.ACTION");
        dwInputIntent.putExtra("com.symbol.datawedge.api.SET_CONFIG", profileConfig);
        dwInputIntent.putExtra("SEND_RESULT", "true");  //  Get feedback from DataWedge
        sendBroadcast(dwInputIntent);

        //  3. Set the Output plugin properties
        profileConfig.remove("PLUGIN_CONFIG");
        Bundle keystrokeConfig = new Bundle();
        keystrokeConfig.putString("PLUGIN_NAME", "KEYSTROKE");
        keystrokeConfig.putString("RESET_CONFIG", "true");
        Bundle keystrokeProps = new Bundle();
        keystrokeProps.putString("keystroke_output_enabled", "true");
        keystrokeConfig.putBundle("PARAM_LIST", keystrokeProps);
        profileConfig.putBundle("PLUGIN_CONFIG", keystrokeConfig);

        Intent dwOutputIntent = new Intent();
        dwOutputIntent.setAction("com.symbol.datawedge.api.ACTION");
        dwOutputIntent.putExtra("com.symbol.datawedge.api.SET_CONFIG", profileConfig);
        dwOutputIntent.putExtra("SEND_RESULT", "true");  //  Get feedback from DataWedge
        sendBroadcast(dwOutputIntent);
	}

    private BroadcastReceiver myBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("com.symbol.datawedge.api.RESULT_ACTION")) {
                //  Process any result codes
                if (intent.hasExtra("RESULT")) {
                    if (intent.hasExtra("COMMAND")) {
                        String result = intent.getStringExtra("RESULT");
                        String command = intent.getStringExtra("COMMAND");
                        String info = "";
                        if (intent.hasExtra("RESULT_INFO")) {
                            Bundle result_info = intent.getBundleExtra("RESULT_INFO");
                            String result_code = result_info.getString("RESULT_CODE");
                            if (result_code != null)
                            {
                                if (result_code.equalsIgnoreCase("APP_ALREADY_ASSOCIATED"))
                                    return;  //  Otherwise the UI gets confusing
                                info = " - " + result_code;
                            }
                        }
                        statusTextView.setText(command + ": " + result + info);
                    }
                } else {
                    statusTextView.setText("No Result associated with this intent");
                }
            }
        }
    };
}

