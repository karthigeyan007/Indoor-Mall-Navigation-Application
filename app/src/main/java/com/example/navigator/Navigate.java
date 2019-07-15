package com.example.navigator;


import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.Manifest;
import android.animation.Animator;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.example.navigator.interfaces.NavigationFragmentInteractionListener;
import com.example.navigator.utils.ArDisplayView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 */

public class Navigate extends Fragment implements SensorEventListener {

    private String TAG = "GameFragment";
    public static final float MOVE_FACTOR_X = 50f;
    public static final float MOVE_FACTOR_Y = MOVE_FACTOR_X * .8f;

    public static final float MOVE_THRESHOLD_ACCEL = 0.01f;
    public static final float MOVE_THRESHOLD_GYRO = 0.01f;

    public static final int STARTING_AR_OBJECT_COUNT = 3;

    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;

    public static final int FIRING_RATE = 200;
    public static final int IMPACT_VIBRATION = 50;
    public static final int ANIMATION_DURATION = 200;
    public static final int SHOT_DELAY = 200;
    public static final int TIME_TO_NEXT_AR = 1000; // Decrease for more trump heads
    public static final int TIME_TO_NEXT_VOTE = 200; // Decrease for faster electoral votes
    public int TIME_TO_NEXT_ELECTION_DAY = 800; // Increase for longer games

    // Decrease for faster electoral votes per trump head
    public static final float AR_COUNT_TIME_FACTOR = 3.5f;

    public static final int TRACKER_ARROW_SIZE = 80;
    public static final int BTC_SIZE = 200;
    public static final int BTC_SIZE_SPACING = BTC_SIZE + 50;

    private NavigationFragmentInteractionListener mListener;
    private View rootView;
    private ViewGroup inflateContainer;
    private LayoutInflater inflater;
    private VideoView landingVideo;
    private View howToPlayContainer;

    private List<View> arObjects;
    private List<View> arLeftTrackerObjects;
    private List<View> arRightTrackerObjects;

    private List<Integer> lastLeftMargins;
    private List<Integer> lastTopMargins;
    private List<Boolean> btcShot;

    private AnimationSet laserAnimSet;

    private int height;
    private int width;
    private int topHitThreshold;
    private int bottomHitThreshold;
    private int leftHitThreshold;
    private int rightHitThreshold;

    private View laserView = null;

    private FrameLayout arContentOverlay = null;

    String accelData = "Accelerometer Data";
    String compassData = "Compass Data";
    String gyroData = "Gyro Data";
    private SensorManager sensorManager;

    private MediaPlayer laserMp;
    private MediaPlayer coinMp;
    private MediaPlayer winMp;
    private MediaPlayer chinaMp;
    private MediaPlayer firedMp;
    private MediaPlayer oyayeMp;
    private MediaPlayer richMp;
    private MediaPlayer bombMp;
    private MediaPlayer loseMp;
    private MediaPlayer founderMp;
    private MediaPlayer mexicansMp;
    private MediaPlayer tittiesMp;

    private boolean arActive = false;
    private boolean gamePaused = false;
    private int votes;
    private int daysToElection;
    private int visibleArObjects;

    private float accelX;
    private float accelY;
    private float accelZ;

    private float gyroX;
    private float gyroY;
    private float gyroZ;

    public Vibrator vibe;
    public LocationManager locationManager;


    private Handler handler = new Handler();

    private Drawable arrowLeft = null;
    private Drawable arrowRight = null;
    private Drawable arrowUp = null;
    private Drawable arrowDown = null;

    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.CAMERA
    };
    private static final int PERMISSIONS_REQUEST_CODE = 1111;
    private RelativeLayout mContainer;

    private boolean havePermissions() {

        if (getContext() != null && PERMISSIONS != null) {
            for (String permission : PERMISSIONS) {
                if (ActivityCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, PERMISSION_ALL);
    }

    private void showLinkToSettingsSnackbar() {
        if (mContainer == null) {
            return;
        }
        Snackbar.make(mContainer,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.settings, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Build intent that displays the App settings screen.
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package",
                                BuildConfig.APPLICATION_ID, null);
                        intent.setData(uri);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }).show();
    }

    private void showRequestPermissionsSnackbar() {
        if (mContainer == null) {
            return;
        }
        Snackbar.make(mContainer, R.string.permission_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Request permission.
                        ActivityCompat.requestPermissions(getActivity(),
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSIONS_REQUEST_CODE);
                    }
                }).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[]permissions, @NonNull int[] grantResults) {
        if (requestCode != PERMISSIONS_REQUEST_CODE) {
            return;
        }

        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                if (shouldShowRequestPermissionRationale(permission)) {
                    Log.i(TAG, "Permission denied without 'NEVER ASK AGAIN': "
                            + permission);
                    showRequestPermissionsSnackbar();
                } else {
                    Log.i(TAG, "Permission denied with 'NEVER ASK AGAIN': "
                            + permission);
                    showLinkToSettingsSnackbar();
                }
            } else {
                Log.i(TAG, "Permission granted, building GoogleApiClient");
            }
        }
    }


    LocationListener locationListener;

    private EditText editLocation = null;

    final Runnable autoGenerateArObjects = new Runnable() {
        @Override
        public void run() {
            createArObjectWithTrackers();
            calculateVisibleArObjects();
            int timeUntilNextArObject = Math.min(visibleArObjects * 100 + TIME_TO_NEXT_AR / 2,
                    TIME_TO_NEXT_AR);
            Log.d(TAG, "timeUntilNextArObject: " + timeUntilNextArObject);

            handler.postDelayed(autoGenerateArObjects, timeUntilNextArObject);
        }
    };

    final Runnable countElectoralVotes = new Runnable() {
        @Override
        public void run() {
            /*votes += 1;
            ((TextView)rootView.findViewById(R.id.electoral_vote_counter)).setText(votes + " / 270");
            int timeToNextVote = (int)Math.round((double)TIME_TO_NEXT_VOTE / (1 +
                    (visibleArObjects / AR_COUNT_TIME_FACTOR)));

            if(votes > 200) {
                ((TextView)rootView.findViewById(R.id.electoral_vote_counter)).setTextColor
                        (ContextCompat.getColor(getContext(), R.color.primary));
            }

            if(votes == 270) {
                gameOver();
            } else {
                handler.postDelayed(countElectoralVotes, timeToNextVote);
            }*/
        }
    };

    final Runnable electionDayCountdown = new Runnable() {
        @Override
        public void run() {
            daysToElection -= 1;
            ((TextView)rootView.findViewById(R.id.days_to_election)).setText(daysToElection + "m");
            if(daysToElection == 0) {
                gameOver();
            } else {
                handler.postDelayed(electionDayCountdown, TIME_TO_NEXT_ELECTION_DAY);
            }
        }
    };

    final Runnable startFiring = new Runnable() {
        @Override
        public void run() {
            laserView.setVisibility(View.VISIBLE);

            laserView.startAnimation(laserAnimSet);
            vibe.vibrate(5);

            if(laserMp.isPlaying()) {
                laserMp.seekTo(0);
                laserMp.start();
            } else {
                laserMp.start();
            }
            handler.postDelayed(shotImpact, ANIMATION_DURATION);
            handler.postDelayed(startFiring, FIRING_RATE);
        }
    };

    final Runnable shotImpact = new Runnable() {
        @Override
        public void run() {
            checkForImpact();
        }
    };

    final Runnable calculateArMoves = new Runnable() {
        @Override
        public void run() {

        }
    };

    final Runnable moveArBtc = new Runnable() {
        @Override
        public void run() {
            if(gamePaused) return;
            setNewMargins();
        }
    };


    public Navigate() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.d(TAG,"onCreateView");

        rootView = inflater.inflate(R.layout.fragment_navigate,container,false);

        editLocation = (EditText) rootView.findViewById(R.id.editTextLocation);

        if (ContextCompat.checkSelfPermission( getContext() ,android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED )
        {
            ActivityCompat.requestPermissions(
                    getActivity(),
                    new String [] { android.Manifest.permission.ACCESS_COARSE_LOCATION },
                    MY_PERMISSION_ACCESS_COARSE_LOCATION
            );
        }

        locationListener = new MyLocationListener();
        locationManager = (LocationManager)
                getContext().getSystemService(Context.LOCATION_SERVICE);



        Boolean flag = displayGpsStatus();
        if (flag) {

            Log.v(TAG, "onClick");



            locationListener = new MyLocationListener();

            if(havePermissions()) {
                //requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_COARSE_LOCATION);
                editLocation.setText("Please!! move your device to" +
                        " see the changes in coordinates." + "\nWait.. 1");
                locationManager.requestLocationUpdates(LocationManager
                        .GPS_PROVIDER, 1000, 0, locationListener);
            }
            else {
                requestPermissions();
            }

        } else {
            Toast.makeText(
                    getContext(),
                    "Your GPS is: OFF", Toast.LENGTH_LONG).show();
        }



        inflateContainer = container;
        this.inflater = inflater;
        howToPlayContainer = rootView.findViewById(R.id.how_to_play_container);
        mListener.timeEvent("App Opened to Play Game");

        configureGameWindow();

        return rootView;
    }

    public void setUIArguments() {}

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof NavigationFragmentInteractionListener) {
            mListener = (NavigationFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate");
        super.onCreate(savedInstanceState);

        arrowLeft = getResources().getDrawable(R.mipmap.arr_left);
        arrowRight = getResources().getDrawable(R.mipmap.arr_right);
        arrowUp = getResources().getDrawable(R.mipmap.arr_up);
        arrowDown = getResources().getDrawable(R.mipmap.arr_down);

    }


    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");

        stopSensing();
        landingVideo.stopPlayback();

    }

    @Override
    public void onStop() {
        Log.d(TAG,"onStop");
        stopAllRunnables();
        sensorManager.unregisterListener(this);
        handler.removeCallbacks(countElectoralVotes);
        handler.removeCallbacks(electionDayCountdown);
        handler.removeCallbacks(autoGenerateArObjects);

        if(winMp != null) winMp.stop();
        if(richMp != null) richMp.stop();
        if(loseMp != null) loseMp.stop();
        if(laserMp != null) laserMp.stop();
        if(chinaMp != null) chinaMp.stop();
        if(oyayeMp != null) oyayeMp.stop();
        if(founderMp != null) founderMp.stop();
        if(mexicansMp != null) mexicansMp.stop();
        if(tittiesMp != null) tittiesMp.stop();


        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG,"onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG,"onDetach");
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
        rootView.findViewById(R.id.instructions_container).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.win_container).setVisibility(View.GONE);
        rootView.findViewById(R.id.ar_container).setVisibility(View.GONE);
        rootView.findViewById(R.id.camera_frame).setVisibility(View.GONE);
        rootView.findViewById(R.id.ar_content_overlay).setVisibility(View.GONE);

        startVideo();

        assignClickListeners();
        applyCustomStyles();
        configureSensors();

    }

    private void applyCustomStyles()
    {

        ((ImageView)rootView.findViewById(R.id.crosshairs)).setImageDrawable(arrowUp);
        laserAnimSet = generateLaserAnimation();

    }

    private void assignClickListeners()
    {
        rootView.findViewById(R.id.play_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onLongClick: ");
                mListener.trackEvent("App Opened to Play Game");

                rootView.findViewById(R.id.instructions_container).setVisibility(View.GONE);
                initializeSound();
                initializeAr();
                prepareNavigation();
            }
        });


        rootView.findViewById(R.id.stop_button).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                onResume();
                return false;
            }
        });

        View.OnClickListener playAgain = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prepareNavigation();
                mListener.trackEvent("Play Again Button Clicked");
            }
        };

        rootView.findViewById(R.id.play_again_win).setOnClickListener(playAgain);

        View.OnClickListener shareApp = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareGeneral();
                mListener.trackEvent("Share Link Clicked");
            }
        };


        rootView.findViewById(R.id.how_to_play).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    howToPlayContainer.setVisibility(View.VISIBLE);
                    mListener.trackEvent("How to Play Clicked");

                    howToPlayContainer.animate().alpha(1f).setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {
                            howToPlayContainer.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {

                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {

                        }
                    });
                }
                return false;
            }
        });
        rootView.findViewById(R.id.dismiss_how_to_play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                howToPlayContainer.animate().alpha(0f).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        howToPlayContainer.setVisibility(View.GONE);

                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {

                    }
                });

            }
        });

    }

    private void calculateVisibleArObjects() {
        int numberOfBtcShot = 0;

        for(int i = 0; i < btcShot.size(); i++) {
            if(btcShot.get(i)) numberOfBtcShot++;
        }

        visibleArObjects = btcShot.size() - numberOfBtcShot;
    }

    private void checkForImpact() {
        /*for(int i = 0; i < btcShot.size(); i++) {
            int leftMargin = lastLeftMargins.get(i);
            int topMargin = lastTopMargins.get(i);

            if(!btcShot.get(i) && topMargin > topHitThreshold && topMargin < bottomHitThreshold &&
                    leftMargin < rightHitThreshold && leftMargin > leftHitThreshold) {
                final View arObject = arObjects.get(i);

                ((ImageView)arObject.findViewById(R.id.ar_object_image)).setImageResource(R
                        .mipmap.trumpexplode);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        arObject.setVisibility(View.GONE);
                    }
                }, SHOT_DELAY);
                vibe.vibrate(IMPACT_VIBRATION);
                playRandomQuote(i);
                btcShot.set(i, true);
            }
        }*/
    }


    private void configureGameWindow() {
        arContentOverlay = (FrameLayout)rootView.findViewById(R.id.ar_content_overlay);
        laserView = rootView.findViewById(R.id.laser);
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        height = display.getHeight();
        width = display.getWidth();
        topHitThreshold = height/2 - (int)(BTC_SIZE*1.1);
        bottomHitThreshold = height/2;
        leftHitThreshold = width/2 - (int)(BTC_SIZE*.9);
        rightHitThreshold = width/2;
    }

    private void configureSensors()
    {
        vibe = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);

        sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        boolean isGyroAvailable = sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
        boolean isAccelAvailable = sensorManager.registerListener(this, accelSensor, SensorManager
                .SENSOR_DELAY_GAME);

        Log.d(TAG, "isGyroAvailable: " +isGyroAvailable);
        Log.d(TAG, "isAccelAvailable: " +isAccelAvailable);

    }

    private void createArObjectWithTrackers() {
        /*btcShot.add(false);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams
                .WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int leftMargin = BTC_SIZE_SPACING + (int)(Math.random() * 1500 * (Math.random() >
                0.5 ? -1 : 1));
        int topMargin = BTC_SIZE_SPACING + (int)(Math.random() * 500 * (Math.random() > 0.5
                ? -1 : 1) + (height / 4));
        lastLeftMargins.add(leftMargin);
        lastTopMargins.add(topMargin);

        View arLayout = inflater.inflate(R.layout.ar_object_trump_view, inflateContainer, false);

        params.setMargins(leftMargin, topMargin, 0, 0);
        arLayout.setLayoutParams(params);
        arContentOverlay.addView(arLayout);
        arObjects.add(arLayout);


        View leftTracker = inflater.inflate(R.layout.ar_object_left_tracker_view, inflateContainer,
                false);
        View rightTracker = inflater.inflate(R.layout.ar_object_right_tracker_view, inflateContainer,
                false);
        ((ImageView)leftTracker.findViewById(R.id.ar_left_tracker_arrow_image)).setImageDrawable(new
                IconicsDrawable(getContext())
                .icon(FontAwesome.Icon.faw_angle_left)
                .color(ContextCompat.getColor(getContext(), R.color.accent)));
        ((ImageView)rightTracker.findViewById(R.id.ar_right_tracker_arrow_image)).setImageDrawable(new
                IconicsDrawable(getContext())
                .icon(FontAwesome.Icon.faw_angle_right)
                .color(ContextCompat.getColor(getContext(), R.color.accent)));

        FrameLayout.LayoutParams arrowParams = new FrameLayout.LayoutParams(ViewGroup
                .LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        leftTracker.setLayoutParams(arrowParams);
        rightTracker.setLayoutParams(arrowParams);
        arContentOverlay.addView(leftTracker);
        arContentOverlay.addView(rightTracker);
        arLeftTrackerObjects.add(leftTracker);
        arRightTrackerObjects.add(rightTracker);*/
    }

    private void initializeAr() {
        arActive = true;

        arContentOverlay.setVisibility(View.VISIBLE);

        FrameLayout arViewPane = (FrameLayout) rootView.findViewById(R.id.camera_frame);
        arViewPane.removeAllViews();
        ArDisplayView arDisplay = new ArDisplayView(getContext());
        arViewPane.addView(arDisplay);
        arViewPane.setVisibility(View.VISIBLE);

    }

    private void initializeGameTimers() {
        daysToElection = 30;
        votes = 0;
        countElectoralVotes.run();
        electionDayCountdown.run();
    }

    private void initializeSound() {
        laserMp = MediaPlayer.create(getContext(), R.raw.laser);
        coinMp = MediaPlayer.create(getContext(), R.raw.coin);
        winMp = MediaPlayer.create(getContext(), R.raw.win);
        chinaMp = MediaPlayer.create(getContext(), R.raw.bigchina);
        firedMp = MediaPlayer.create(getContext(), R.raw.fired);
        oyayeMp = MediaPlayer.create(getContext(), R.raw.oyaye);
        richMp = MediaPlayer.create(getContext(), R.raw.rich);
        bombMp = MediaPlayer.create(getContext(), R.raw.bomb);
        loseMp = MediaPlayer.create(getContext(), R.raw.lose);
        founderMp = MediaPlayer.create(getContext(), R.raw.founder);
        mexicansMp = MediaPlayer.create(getContext(), R.raw.mexicans);
        tittiesMp = MediaPlayer.create(getContext(), R.raw.titties);

        laserMp.setVolume(0.07f, 0.07f);


    }

    private void gameOver() {

        rootView.findViewById(R.id.stop_button).setVisibility(View.GONE);
        rootView.findViewById(R.id.crosshairs).setVisibility(View.GONE);
        /*for(int i = 0; i < btcShot.size(); i++) {
            arLeftTrackerObjects.get(i).setVisibility(View.GONE);
            arRightTrackerObjects.get(i).setVisibility(View.GONE);
        }*/
        handler.removeCallbacks(autoGenerateArObjects);
        handler.removeCallbacks(countElectoralVotes);
        handler.removeCallbacks(electionDayCountdown);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                rootView.findViewById(R.id.win_container).setVisibility(View.VISIBLE);
                mListener.trackEvent("Game Won");
                winMp.start();
            }
        }, 500);


        gamePaused = true;
    }

    private void generateArObjects() {

        arObjects = new ArrayList<>();
        arLeftTrackerObjects = new ArrayList<>();
        arRightTrackerObjects = new ArrayList<>();
        lastLeftMargins = new ArrayList<>();
        lastTopMargins = new ArrayList<>();
        btcShot = new ArrayList<>();

        for(int i = 0; i < STARTING_AR_OBJECT_COUNT; i++) {
            createArObjectWithTrackers();
        }

        autoGenerateArObjects.run();

        gamePaused = false;

    }

    private AnimationSet generateLaserAnimation() {
        AnimationSet animSet = new AnimationSet(true);

        ScaleAnimation scaleAnimationShrink = new ScaleAnimation(10f, 0, 20f, 0, Animation
                .RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimationShrink.setFillAfter(true);

        scaleAnimationShrink.setDuration(ANIMATION_DURATION);

        TranslateAnimation transAnim = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0f,
                Animation.RELATIVE_TO_PARENT, 0f,
                Animation.RELATIVE_TO_PARENT, 0.45f,
                Animation.RELATIVE_TO_PARENT, 0.025f);
        transAnim.setFillAfter(true);
        transAnim.setInterpolator(new DecelerateInterpolator(1000f));

        transAnim.setDuration(ANIMATION_DURATION);

        animSet.addAnimation(transAnim);
        animSet.addAnimation(scaleAnimationShrink);
        animSet.setFillAfter(true);
        animSet.setInterpolator(new DecelerateInterpolator(1f));
        return animSet;
    }

    private void moveArFrameGyro(float x, float y, float z) {

        if(x > MOVE_THRESHOLD_GYRO) {
            ArrayList<Integer> newTopMargins = new ArrayList<>();

            /*for(int i = 0; i < arObjects.size(); i++) {
                int topMargin = lastTopMargins.get(i) + (int)(1 + MOVE_FACTOR_X * x);
                newTopMargins.add(topMargin);
            }*/
            lastTopMargins = newTopMargins;

            handler.post(moveArBtc);

        }
        if(x < MOVE_THRESHOLD_GYRO * -1) {

            ArrayList<Integer> newTopMargins = new ArrayList<>();

            /*for(int i = 0; i < arObjects.size(); i++) {
                int topMargin = lastTopMargins.get(i) - (int)(1 + MOVE_FACTOR_X * x * -1);
                newTopMargins.add(topMargin);
            }*/
            lastTopMargins = newTopMargins;

            handler.post(moveArBtc);

        }

        if(y > MOVE_THRESHOLD_GYRO) {

            ArrayList<Integer> newLeftMargins = new ArrayList<>();

            /*for(int i = 0; i < arObjects.size(); i++) {
                int leftMargin = lastLeftMargins.get(i) + (int)(1 + MOVE_FACTOR_Y * y);
                newLeftMargins.add(leftMargin);
            }*/
            lastLeftMargins = newLeftMargins;

            handler.post(moveArBtc);

        }

        if(y < MOVE_THRESHOLD_GYRO * -1) {

            ArrayList<Integer> newLeftMargins = new ArrayList<>();

           /* for(int i = 0; i < arObjects.size(); i++) {
                int leftMargin = lastLeftMargins.get(i) - (int)(1 + MOVE_FACTOR_Y * y * -1);
                newLeftMargins.add(leftMargin);
            }*/
            lastLeftMargins = newLeftMargins;

            handler.post(moveArBtc);

        }

    }

    private void moveArFrameAccel(float x, float y, float z) {

        if(x > MOVE_THRESHOLD_GYRO) {
            Log.d(TAG, "Move down: " + String.format("%.4f", x));

            handler.post(moveArBtc);

        }
        if(x < MOVE_THRESHOLD_GYRO * -1) {
            Log.d(TAG, "Move up: " + String.format("%.4f", x));
            handler.post(moveArBtc);

        }

        if(y > MOVE_THRESHOLD_GYRO) {
            Log.d(TAG, "Move right: " + String.format("%.4f", y));
            handler.post(moveArBtc);

        }

        if(y < MOVE_THRESHOLD_GYRO * -1) {
            Log.d(TAG, "Move left: " + String.format("%.4f", y));


            handler.post(moveArBtc);

        }

    }

    private void playRandomQuote(int index) {
        switch (index % 8) {
            case 0:
                chinaMp.start(); break;
            case 1:
                richMp.start(); break;
            case 2:
                oyayeMp.start(); break;
            case 3:
                firedMp.start(); break;
            case 4:
                bombMp.start(); break;
            case 5:
                founderMp.start(); break;
            case 6:
                mexicansMp.start(); break;
            case 7:
                tittiesMp.start(); break;
        }
    }

    private void prepareNavigation() {
        arContentOverlay.removeAllViews();

        rootView.findViewById(R.id.instructions_container).setVisibility(View.GONE);
        rootView.findViewById(R.id.how_to_play_container).setVisibility(View.GONE);
        rootView.findViewById(R.id.win_container).setVisibility(View.GONE);
        rootView.findViewById(R.id.ar_container).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.stop_button).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.crosshairs).setVisibility(View.VISIBLE);
        ((TextView)rootView.findViewById(R.id.electoral_vote_counter)).setTextColor
                (ContextCompat.getColor(getContext(), R.color.white));

        landingVideo.stopPlayback();
        landingVideo.setVisibility(View.GONE);

        //generateArObjects();
        initializeGameTimers();
    }

    private void setNewMargins() {
        /*for(int i = 0; i < arObjects.size(); i++) {
            View arView = arObjects.get(i);
            int leftMargin = lastLeftMargins.get(i);
            int topMargin =lastTopMargins.get(i);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(arView.getLayoutParams());
            params.setMargins(leftMargin, topMargin, 0, 0);
            arView.setLayoutParams(params);
            if(!btcShot.get(i)) {
                View leftTrackerArrow = arLeftTrackerObjects.get(i);
                View rightTrackerArrow = arRightTrackerObjects.get(i);

                if(leftMargin < BTC_SIZE * -1) {
                    leftTrackerArrow.setVisibility(View.VISIBLE);
                    FrameLayout.LayoutParams leftTrackerArrowParams = new FrameLayout
                            .LayoutParams(leftTrackerArrow.getLayoutParams());
                    leftTrackerArrowParams.setMargins(0, Math.max(0, topMargin), 0, 0);
                    leftTrackerArrow.setLayoutParams(leftTrackerArrowParams);
                } else {
                    leftTrackerArrow.setVisibility(View.INVISIBLE);
                }
                if(leftMargin > width) {
                    rightTrackerArrow.setVisibility(View.VISIBLE);
                    FrameLayout.LayoutParams rightTrackerArrowParams = new FrameLayout
                            .LayoutParams(rightTrackerArrow.getLayoutParams());
                    rightTrackerArrowParams.setMargins(width - TRACKER_ARROW_SIZE, Math.max
                            (0, topMargin), 0, 0);
                    rightTrackerArrow.setLayoutParams(rightTrackerArrowParams);

                } else {
                    rightTrackerArrow.setVisibility(View.INVISIBLE);
                }
            }
        }*/
    }

    private void shareGeneral() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=" + getContext
                ().getPackageName());
        startActivity(Intent.createChooser(shareIntent, "Share link using"));
    }

    private void startVideo() {
        landingVideo = (VideoView)rootView.findViewById(R.id.landing_video);
        landingVideo.setVideoURI(Uri.parse("android.resource://" + getContext().getPackageName()
                + "/" + R.raw.compass));
        landingVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.setLooping(true);
                mediaPlayer.setVolume(0, 0);
                landingVideo.start();
            }
        });
        landingVideo.setVisibility(View.VISIBLE);

    }


    private void stopAllRunnables()
    {
    }

    private void stopSensing() {
        sensorManager.unregisterListener(this);
    }

    private void updateSensorUi() {

//        ((TextView)rootView.findViewById(R.id.accel_text_x)).setText(String.format("%.4f", accelX));
//        ((TextView)rootView.findViewById(R.id.accel_text_y)).setText(String.format("%.4f", accelY));
//        ((TextView)rootView.findViewById(R.id.accel_text_z)).setText(String.format("%.4f", accelZ));
//
//        ((TextView)rootView.findViewById(R.id.gyro_text_x)).setText(String.format("%.4f", gyroX));
//        ((TextView)rootView.findViewById(R.id.gyro_text_y)).setText(String.format("%.4f", gyroY));
//        ((TextView)rootView.findViewById(R.id.gyro_text_z)).setText(String.format("%.4f", gyroZ));
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch(sensorEvent.sensor.getType())
        {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                accelX = sensorEvent.values[0];
                accelY = sensorEvent.values[1];
                accelZ = sensorEvent.values[2];

                updateSensorUi();
//                if(arActive) moveArFrameAccel(accelX, accelY, accelZ);

                break;
            case Sensor.TYPE_GYROSCOPE:

                gyroX = sensorEvent.values[0];
                gyroY = sensorEvent.values[1];
                gyroZ = sensorEvent.values[2];

                updateSensorUi();
                if(arActive) moveArFrameGyro(gyroX, gyroY, gyroZ);

                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.d(TAG, "onAccuracyChanged");
        Log.d(TAG, sensor.toString());

        switch(sensor.getType())
        {
            case Sensor.TYPE_ACCELEROMETER:
                break;
            case Sensor.TYPE_GYROSCOPE:
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                break;
        }

    }

    /*----Method to Check GPS is enable or disable ----- */
    private Boolean displayGpsStatus() {
        ContentResolver contentResolver = getContext()
                .getContentResolver();
        boolean gpsStatus = Settings.Secure
                .isLocationProviderEnabled(contentResolver,
                        LocationManager.GPS_PROVIDER);
        if (gpsStatus) {
            return true;

        } else {
            return false;
        }
    }

    /*---------- Listener class to get coordinates ------------- */
    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            editLocation.setText("");
            /*Toast.makeText(
                    getContext(),
                    "Location changed: Lat: " + loc.getLatitude() + " Lng: "
                            + loc.getLongitude(), Toast.LENGTH_SHORT).show();*/
            String longitude = "Longitude: " + loc.getLongitude();
            Log.v(TAG, longitude);
            String latitude = "Latitude: " + loc.getLatitude();
            Log.v(TAG, latitude);

            /*------- To get city name from coordinates -------- */
            String cityName = null;
            Geocoder gcd = new Geocoder(getContext(), Locale.getDefault());
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(loc.getLatitude(),
                        loc.getLongitude(), 1);
                if (addresses.size() > 0) {
                    System.out.println(addresses.get(0).getLocality());
                    cityName = addresses.get(0).getLocality();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            String s = longitude + "\n" + latitude + "\n\nMy Current City is: "
                    + cityName;
            editLocation.setText(s);
        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }

}
