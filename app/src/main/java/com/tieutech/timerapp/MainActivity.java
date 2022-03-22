package com.tieutech.timerapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    //============ DECLARE/INITIALISE VARIABLES ==================================
    //Views
    TextView lastWorkoutInfoTextView;
    TextView timerTextView;
    ImageView playImageView;
    ImageView pauseImageView;
    ImageView stopImageView;
    EditText workoutTypeEditText;

    //Keys for Bundle (savedInstanceState)
    // For saving/retrieving data between state changes
    String STATE_MILLIS = "state_milli";
    String STATE_SECONDS = "state_seconds";
    String STATE_MINUTES = "state_minutes";
    String STATE_HOURS = "state_hours";
    String STATE_IS_TIMER_RUNNING = "state_is_timer_running";
    String STATE_HAS_TIMER_STARTED = "state_has_timer_started";
    String STATE_WORKOUT_TYPE = "state_workout_type";
    String STATE_LAST_WORKOUT_INFO = "state_last_workout_info";
    String STATE_PRESSED_BUTTON = "pressed_button";

    //Keys for Shared Preferences
    // For saving/retrieving persistent data in the hard drive between app destruction
    String SHARED_PREF_DATA = "shared_pref_data";
    String SHARED_PREF_WORKOUT_TYPE = "shared_pref_workout_type";
    String SHARED_PREF_LAST_WORKOUT_INFO = "shared_pref_last_workout_info";

    //Flags to indicate state of the timer
    boolean hasTimerStarted = false; //Flag to indicate if the timer has started (i.e. started AND currently running or paused)
    boolean isTimerRunning = false; //Flag to indicate if the timer is currently running (i.e. started but currently not paused)

    //Timer values
    long millis = 0; //Cumulative total ms lapsed by the timer
    int seconds = 0;
    int minutes = 0;
    int hours = 0;
    long startTime = 0; //Time in ms lapsed since January 1, 1970 UTC at the point when the timer was last STARTED
    long pausedTime = 0; //Total ms lapsed by the timer at the point when it was last PAUSED

    //TextView String values
    String workoutType = "";
    String lastWorkoutInfo = ""; //Text to display the last workout, e.g. "You spent 8:36 on sit-ups last time"

    //Other variables
    Handler timerHandler = new Handler(); //Handler to handle the timer thread
    int[] pressedButton = {0, 0, 0}; //Array that identifies the button that has been faded

    //============ DEFINE FUNCTIONS ===================================
    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i("systemState", "onCreate called"); //Verify if onCreate() is called

        //Obtain Views
        lastWorkoutInfoTextView = (TextView) findViewById(R.id.lastWorkoutInfoTextView);
        timerTextView = (TextView) findViewById(R.id.timerTextView);
        playImageView = (ImageView) findViewById(R.id.playImageView);
        pauseImageView = (ImageView) findViewById(R.id.pauseImageView);
        stopImageView = (ImageView) findViewById(R.id.stopImageView);
        workoutTypeEditText = (EditText) findViewById(R.id.workoutTypeEditText);
        timerTextView = (TextView) findViewById(R.id.timerTextView);

        //Obtain the Bundle of data (savedInstanceState) if it exists
        // NOTE: The below block of code retrieves any saved data upon a state change resulting
        // from configuration changes, e.g. change in screen orientation
        if (savedInstanceState != null) {
            //VERIFICATION CHECKPOINT
            Log.i("savedInstanceState", "NOT null"); //Verify if Bundle exists

            //Retrieve Bundled data saved upon a configuration change
            millis = savedInstanceState.getLong(STATE_MILLIS);
            seconds = savedInstanceState.getInt(STATE_SECONDS);
            minutes = savedInstanceState.getInt(STATE_MINUTES);
            hours = savedInstanceState.getInt(STATE_HOURS);
            isTimerRunning = savedInstanceState.getBoolean(STATE_IS_TIMER_RUNNING);
            hasTimerStarted = savedInstanceState.getBoolean(STATE_HAS_TIMER_STARTED);
            workoutType = savedInstanceState.getString(STATE_WORKOUT_TYPE);
            lastWorkoutInfo = savedInstanceState.getString(STATE_LAST_WORKOUT_INFO);
            pressedButton = savedInstanceState.getIntArray(STATE_PRESSED_BUTTON);

            //If the timer has started and is currently running
            if (hasTimerStarted && isTimerRunning) {
                resumeTimer(); //Resume the timer (i.e. continue the count-up timer)
            }

            //If the timer has started and is currently paused
            if (hasTimerStarted && !isTimerRunning) {
                timerTextView.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            }

            //If text information of the last workout EXISTS (e.g.
            if (!lastWorkoutInfo.isEmpty()) {
                lastWorkoutInfoTextView.setText(lastWorkoutInfo); //Display text information of the last workout
            }


            normaliseAllImageViews(); //Un-fade all imageView views (i.e. start timer, pause timer, and stop timer buttons)
            if (pressedButton[0] == 1) {
                playImageView.animate().alpha(0.25f).setDuration(200); //Fade the playImageView
            }
            if (pressedButton[1] == 1) {
                pauseImageView.animate().alpha(0.25f).setDuration(200); //Fade the playImageView
            }
            if (pressedButton[2] == 1) {
                stopImageView.animate().alpha(0.25f).setDuration(200); //Fade the playImageView
            }
        }

        //Obtain any SharedPreferences if they exist
        // NOTE: The below section of code retrieves any saved data on the HARD DRIVE resulting
        // from app destruction, i.e. when the user clears the app in the app stack and opens it up again

        //Created a SharedPreference based on the key specified by SHARED_PREF_DATA
        SharedPreferences prefs = getSharedPreferences(SHARED_PREF_DATA, MODE_PRIVATE);

        workoutType = prefs.getString(SHARED_PREF_WORKOUT_TYPE, ""); //Obtain the workout type, e.g. "push up"
        lastWorkoutInfo = prefs.getString(SHARED_PREF_LAST_WORKOUT_INFO, ""); //Obtain text information of the last workout

        //If the workout type EXISTS
        if (!workoutType.isEmpty()) {
            lastWorkoutInfoTextView.setText(lastWorkoutInfo); //Display text information of the last workout
            workoutTypeEditText.setText(workoutType); //Display the workout type in the EditText
        }
    }

    //Called upon a change in configuration, e.g. change in screen orientation
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        //Save timer values
        savedInstanceState.putLong(STATE_MILLIS, millis);
        savedInstanceState.putInt(STATE_SECONDS, seconds);
        savedInstanceState.putInt(STATE_MINUTES, minutes);
        savedInstanceState.putInt(STATE_HOURS, hours);

        //Save flags of timer states
        savedInstanceState.putBoolean(STATE_IS_TIMER_RUNNING, isTimerRunning);
        savedInstanceState.putBoolean(STATE_HAS_TIMER_STARTED, hasTimerStarted);

        //Save TextView String values
        savedInstanceState.putString(STATE_WORKOUT_TYPE, workoutType);
        savedInstanceState.putString(STATE_LAST_WORKOUT_INFO, lastWorkoutInfo);

        //Save button pressed array
        savedInstanceState.putIntArray(STATE_PRESSED_BUTTON, pressedButton);
    }

    //Create a Runnable to run the timer thread
    Runnable timerRunnable = new Runnable() {
        @SuppressLint("DefaultLocale")

        //Override the run() method of the Runnable class to define what happens every 500ms in this thread
        @Override
        public void run() {

            //Obtain the cumulative total ms lapsed by the timer
            // 'System.currentTimeMillis()': ms lapsed since January 1, 1970 UTC
            // 'startTime': ms lapsed since January 1, 1970 UTC at the point when the timer was last STARTED
            // 'pausedTime': total ms lapsed by the timer at the point when it was last PAUSED
            millis = (System.currentTimeMillis() - startTime) + pausedTime;

            seconds = (int) (millis/1000); //Extrapolate the second(s) lapsed by the timer
            minutes = seconds/60; //Extrapolate the minute(s) lapsed by the timer
            hours = seconds/3600; //Extrapolate the hours(s) lapsed by the timer
            seconds = seconds%60; //Extrapolate the 'remainder' second(s) lapsed by the timer after dividing by 60
            minutes = minutes&60; //Extrapolate the 'remainder' minutes(s) lapsed by the timer after dividing by 60

            timerTextView.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds)); //Display the timer

            //Add the timer Runnable to the message queue, and run it after 500s lapses
            timerHandler.postDelayed(this, 500);
        }
    };

    //Resume the timer (e.g. after it has been paused)
    public void resumeTimer() {
        pausedTime = millis; //Save the cumulative total ms lapsed by the timer at the point when it was last PAUSED
        startTime = System.currentTimeMillis(); //Obtain ms lapsed since January 1, 1970 UTC at the point when the timer was last STARTED

        timerHandler.postDelayed(timerRunnable, 0); //Start the timer Runnable thread

        isTimerRunning = true; //Indicate that the timer is currently running
    }

    //Un-fade all imageView views (i.e. start timer, pause timer, and stop timer buttons)
    public void normaliseAllImageViews() {
        playImageView.animate().alpha(1.0f).setDuration(400);
        pauseImageView.animate().alpha(1.0f).setDuration(400);
        stopImageView.animate().alpha(1.0f).setDuration(400);
    }

    public void activatePressedButton(int buttonNumber) {
        for (int i=0; i<pressedButton.length-1; i++) {
            pressedButton[i] = 0;
        }
        pressedButton[buttonNumber] = 1;
    }

    //Onclick listener for the playImageView view
    // FUNCTION: Starts/Resumes the timer
    public void startTimer(View view) {
        normaliseAllImageViews(); //Un-fade all imageView views (i.e. start timer, pause timer, and stop timer buttons)
        activatePressedButton(0);
        playImageView.animate().alpha(0.25f).setDuration(200); //Fade the playImageView

        //If the timer has NOT been started (e.g. it is at "00:00" since it was either never started OR was stopped)
        if (!hasTimerStarted) {

            startTime = System.currentTimeMillis(); //Obtain the ms lapsed since January 1, 1970 UTC
            timerHandler.postDelayed(timerRunnable, 0); //Start the timer Runnable thread

            hasTimerStarted = true; //Indicate that the timer has been started (i.e. it is NOT on "00:00")
            isTimerRunning = true; //Indicate that the timer is currently running
        }
        //If the timer has been started (e.g. it is currently running OR has been paused after being started)
        else {
            resumeTimer(); //Resume the timer
        }
    }

    //Onclick listener for the pauseImageView view
    // FUNCTION: Pauses the timer
    public void pauseTimer(View view) {
        normaliseAllImageViews(); //Un-fade all imageView views (i.e. start timer, pause timer, and stop timer buttons)
        activatePressedButton(1);
        pauseImageView.animate().alpha(0.25f).setDuration(200); //Fade the playImageView


        pausedTime = 0; //Save the cumulative total ms lapsed by the timer at the point when it was last PAUSED
        startTime = System.currentTimeMillis(); //Obtain ms lapsed since January 1, 1970 UTC at the point when the timer was last STARTED

        timerHandler.removeCallbacks(timerRunnable); //Remove the timer Handler thread to halt the timer from counting up

        hasTimerStarted = true; //Indicate that the timer has been started (i.e. it is NOT on "00:00")
        isTimerRunning = false; //Indicate that the timer is currently NOT running
    }

    //Onclick listener for the stopImageView view
    // FUNCTION: Stops the timer
    @SuppressLint("DefaultLocale")
    public void stopTimer(View view) {
        normaliseAllImageViews(); //Un-fade all imageView views (i.e. start timer, pause timer, and stop timer buttons)
        activatePressedButton(2);
        stopImageView.animate().alpha(0.25f).setDuration(200); //Fade the playImageView


        millis = 0; //Reset the cumulative total ms lapsed by the timer
        timerHandler.removeCallbacks(timerRunnable); //Remove the timer Handler thread to halt the timer from counting up

        timerTextView.setText(getResources().getString(R.string.zero_time_text)); //Set the TextView of the timer to zero (e.g. "00:00")

        //Update the display of the information of the last workout
        workoutType = workoutTypeEditText.getText().toString(); //Obtain the text inputted into workoutTypeEditText (i.e. workout type)

        //If nothing has been input into lastWorkoutInfoTextView by the user
        if (workoutType.isEmpty()) {
            lastWorkoutInfo = "You spent " + String.format("%02d:%02d", minutes, seconds) + " on *some workout* last time."; //Generate the String for the information of the last (unnamed) workout
        }
        else {
            lastWorkoutInfo = "You spent " + String.format("%02d:%02d", minutes, seconds) + " on " + workoutType + " last time."; //Generate the String for the information of the last workout
        }

        lastWorkoutInfoTextView.setText(lastWorkoutInfo); //Display the information of the last workout

        hasTimerStarted = false; //Indicate that the timer has not been started (i.e. it is on "00:00")
        isTimerRunning = false; //Indicate that the timer is currently NOT running

    }

    //onPause() lifecycle callback
    //NOTE: This is called when the activity comes into the foreground
    @Override
    public void onPause() {
        super.onPause();
        Log.i("systemState", "onPause() called"); //Verify if onStart() lifecycle callback is executed

        timerHandler.removeCallbacks(timerRunnable); //Remove the timer Handler thread to halt the timer from counting up
    }

    //onStop() lifecycle callback
    //NOTE: This is called when the activity is no longer visible
    @Override
    public void onStop() {
        super.onStop();
        Log.i("systemState", "onStop() called"); //Verify if onStart() lifecycle callback is executed

        //Save primitive data to the hard drive (to be retrieved when the app opens up again (i.e. when onCreate(..) is called again))
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(SHARED_PREF_DATA, MODE_PRIVATE); //Create SharedPreference object to access hard drive
        SharedPreferences.Editor editor = sharedPreferences.edit(); //Create SharedPreferences.Editor to edit the SharedPreference
        editor.putString(SHARED_PREF_WORKOUT_TYPE, workoutType); //Add the key-value pair for the workout type to the SharedPreference
        editor.putString(SHARED_PREF_LAST_WORKOUT_INFO, lastWorkoutInfo); //Add the key-value pair for the information of the last workout to the SharedPreference
        editor.apply(); //Commit SharedPreferences changes to hard drive
    }

    //onDestroy() lifecycle callback
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("systemState", "onDestroy() called"); //Verify if onStart() lifecycle callback is executed
    }
}