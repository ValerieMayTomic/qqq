/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothchat;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Random;

import com.example.android.common.logger.Log;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothChatFragment extends Fragment {

    private static final String TAG = "BluetoothChatFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    private static Scanner input;
    private static ArrayList<Question> questions = new ArrayList<Question>();

    private static int current = 0;

    private static Random numbers;


    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;
    static private Context ctx;




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        ctx = getActivity();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }

    // Declare layout elements from xml file
    private Button mStartButton;
    private LinearLayout startLayout;
    private RelativeLayout entryLayout;
    private TextView displayQuestion;
    private Button submitButton;
    private EditText inputAnswer;
    private RelativeLayout finalLayout;
    private TextView correctAnswerText;
    private TextView playerOneScore;
    private TextView playerTwoScore;
    private Button playAnotherRound;

    private int currentQuestion = 0;
    private static int p1 = 0;
    private static int p2 = 0;
    private static int received = -1;
    private static boolean goAgain = false;
    private static boolean alreadyIncrement1 = false;
    private static boolean alreadyIncrement2 = false;
    @Override

    //CREATION OF LAYOUT RELATED ETC
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
       // Initialize layot elements from xml file
        mStartButton = (Button) view.findViewById(R.id.button_start);
        startLayout = (LinearLayout) view.findViewById(R.id.start_layout);
        entryLayout = (RelativeLayout) view.findViewById(R.id.entry_layout);
        displayQuestion = (TextView) view.findViewById(R.id.displayQuestion);
        submitButton = (Button) view.findViewById(R.id.submit);
        inputAnswer = (EditText) view.findViewById(R.id.inputAnswer);
        finalLayout = (RelativeLayout) view.findViewById(R.id.final_layout);
        correctAnswerText = (TextView) view.findViewById(R.id.correctAnswer);
        playerOneScore = (TextView) view.findViewById(R.id.playerOneScore);
        playerTwoScore = (TextView) view.findViewById(R.id.playerTwoScore);
        playAnotherRound = (Button) view.findViewById(R.id.playAnotherRound);
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);



            goAgain = false;

            //start button to begin game
            mStartButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    View view = getView();

                    if (null != view) {
                        //Check if bluetooth connection has been established
                        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
                            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        //read in questions and select one at random
                        input = new Scanner(System.in);

                        // Set up ArrayList of questions with ID number, question, and answer
                        questions.add(new Question(0, "How many feet tall was the tallest giraffe ever recorded?", 20));
                        questions.add(new Question(1, "In what year did Benjamin Franklin prove that lightning was electricity, after flying his kite in a thunderstorm?", 1752));
                        questions.add(new Question(2, "In what year did MTV play its first music video?", 1981));
                        questions.add(new Question(3, "Greenland is the world's largest island. What percent of it is continuously covered with a sheet of ice?", 81));
                        questions.add(new Question(4, "How many bones are in adult human body?", 206));
                        questions.add(new Question(5, "How many paintings did Vincent van Gogh sell during his lifetime?", 1));
                        questions.add(new Question(6, "In dollars, what was Julia Roberts' salary for her role in the hit movie Pretty Women?", 300000));
                        questions.add(new Question(7, "In pounds, what was the weight of the largest gold nugget ever found?", 156));
                        questions.add(new Question(8, "How many men signed the Declaration of Independance?", 56));
                        questions.add(new Question(9, "How many calories are there in a standard 12-ounce can of Coca-Cola Classic?", 140));
                        questions.add(new Question(10, "As of 2005, what was the least number of combined points scored by both teams in a Super Bowl?", 21));
                        questions.add(new Question(11, "How many feet tall is the Empire State Building?", 1250));
                        questions.add(new Question(12, "In what year did the first episode of the animated sitcom The Simpsons air?", 1989));
                        questions.add(new Question(13, "In dollars, how much does the average American dog owner spend on a dog over its lifetime?", 31995));
                        questions.add(new Question(14, "In what year did the repeal of Prohibition make it legal once again to sell alcohol in the US?", 1933));
                        questions.add(new Question(15, "How many official languages are used by the United Nations?", 6));
                        questions.add(new Question(16, "In what year was penicillin, the first antibiotic, discovered?", 1928));
                        questions.add(new Question(17, "How many US $1 bills would be contained in a stack of bills one foot high?", 2746));
                        questions.add(new Question(18, "In what year was the film Citizen Kane first released?", 1941));
                        questions.add(new Question(19, "How many artists made up USA for Africa, the group that recorded the 1985 hit song We Are the World?", 45));

                        // Find current question by ID number and display on screen
                        for (Question q : questions) {
                            if (q.getID() == currentQuestion)
                                displayQuestion.setText(q.getQuestion());
                        }

                        // Switch to next page where user can enter answer
                        startLayout.setVisibility(View.GONE);
                        entryLayout.setVisibility(View.VISIBLE);
                    }
                }
            });

            // Initialize the BluetoothChatService to perform bluetooth connections
            mChatService = new BluetoothChatService(getActivity(), mHandler);

            // Initialize the buffer for outgoing messages
            mOutStringBuffer = new StringBuffer("");

            // Reactions to submit button
            submitButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    View view = getView();

                    if (null != view) {
                        String answer = inputAnswer.getText().toString();

                        // Once user enters answer, send a Bluetooth message to the connected device with your score
                        // 5 points for correct answer
                        // 0 points for incorrect answer
                        sendMessage(answer);

                        // Once a message is received, switch to the final score screen
                        if (received > 0) {

                            Log.i("TAG", Integer.toString(received));

                            entryLayout.setVisibility(View.GONE);
                            finalLayout.setVisibility(View.VISIBLE);

                            // Increment point value accordingly for you and your opponent
                            for (Question q : questions) {
                                if (q.getID() == currentQuestion) {
                                    correctAnswerText.setText(Integer.toString(q.getAnswer()));
                                    if (Integer.toString(q.getAnswer()).equals(answer) && alreadyIncrement1 == false) {
                                        p1 += 5;
                                        alreadyIncrement1 = true;
                                    }
                                    if (Integer.toString(q.getAnswer()).equals(Integer.toString(received)) && alreadyIncrement2 == false) {
                                        p2 += 5;
                                        alreadyIncrement2 = true;
                                    }
                                    playerOneScore.setText(Integer.toString(p1));
                                    playerTwoScore.setText(Integer.toString(p2));
                                }
                            }

                        }


                    }
                }
            });

            // If users choose to play again, restore the start screen
            playAnotherRound.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    View view = getView();

                    if (null != view) {

                        goAgain = true;
                        currentQuestion++;

                        finalLayout.setVisibility(View.GONE);
                        startLayout.setVisibility(View.VISIBLE);

                        alreadyIncrement1 = false;
                        alreadyIncrement2 = false;


                    }
                }
            });
    }

    /**
     * Makes this device discoverable.
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {

            byte[] send = message.getBytes();
            mChatService.write(send);


    }


    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.i("TAG", readMessage);
                    received = Integer.parseInt(readMessage);

                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other divice
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                //  the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

}
