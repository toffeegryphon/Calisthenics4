package com.hylux.calisthenics4;

import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hylux.calisthenics4.homeview.ChooseWorkoutFragment;
import com.hylux.calisthenics4.homeview.CreateWorkoutFragment;
import com.hylux.calisthenics4.homeview.RecentActivitiesFragment;
import com.hylux.calisthenics4.objects.Exercise;
import com.hylux.calisthenics4.objects.Workout;
import com.hylux.calisthenics4.roomdatabase.ActivitiesDatabase;
import com.hylux.calisthenics4.roomdatabase.ActivitiesViewModel;
import com.hylux.calisthenics4.roomdatabase.OnTaskCompletedListener;
import com.hylux.calisthenics4.workoutview.SwipeViewPagerAdapter;
import com.hylux.calisthenics4.workoutview.ToggleSwipeViewPager;
import com.hylux.calisthenics4.workoutview.WorkoutActivity;

import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements OnTaskCompletedListener {

    public static final int NEW_WORKOUT_REQUEST = 0;

    private ActivitiesViewModel activitiesViewModel;

    private ArrayList<Fragment> fragments;
    private ViewPager viewPager;
    private PagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        onTaskCompletedListener = this;

//        final Button debugButton = findViewById(R.id.debugButton);
//        debugButton.setOnClickListener(new View.OnClickListener() { //TODO Maybe can replace with lambda
//            @Override
//            public void onClick(View v) {
//                Intent debugActivityIntent = new Intent(MainActivity.this, WorkoutActivity.class);
//                Workout debugWorkout = Debug.debugWorkout();
//                debugActivityIntent.putExtra("EXTRA_WORKOUT", debugWorkout);
//                startActivityForResult(debugActivityIntent, NEW_WORKOUT_REQUEST);
//            }
//        });

        //Firebase Firestore database for templates
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        addExercise(Debug.debugExercise(), database);

        //Room database for actual activities
        ActivitiesDatabase activitiesDatabase = ActivitiesDatabase.getDatabase(getApplicationContext());
        activitiesViewModel = new ActivitiesViewModel(getApplication());

        //Instantiate fragments
        fragments = new ArrayList<>();
        fragments.add(RecentActivitiesFragment.newInstance(new ArrayList<Workout>()));
        fragments.add(ChooseWorkoutFragment.newInstance());
        fragments.add(CreateWorkoutFragment.newInstance());

        //Set up SwipeViewPager
        viewPager = findViewById(R.id.swipeViewPager);
        adapter = new SwipeViewPagerAdapter(getSupportFragmentManager(), fragments);
        //TODO disallow drag if on create workout.
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(2);
        ((ToggleSwipeViewPager) viewPager).setCanSwipe(false);

        activitiesViewModel.getRecentActivities(5,this);

//        final ImageButton refreshButton = findViewById(R.id.refreshButton);
//        refreshButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d("REFRESH", "onClick()");
//                activitiesViewModel.getRecentActivities(5, onTaskCompletedListener);
//            }
//        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == NEW_WORKOUT_REQUEST) {
            if (resultCode == RESULT_OK) {
                Workout activity = Objects.requireNonNull(data).getParcelableExtra("EXTRA_WORKOUT");
                activitiesViewModel.insert(activity);
            }
        }
    }

    private void addExercise(Exercise exercise, final FirebaseFirestore database) {
        //TODO Check if item exists (maybe can do by initializing id to 0 first)
        //TODO Do a further check if ID exists
        final Exercise addedExercise = exercise;
        if (exercise.getId().equals("default")) {
            database.collection("exercises")
                    .add(Debug.debugExercise())
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            String exerciseId = documentReference.getId();
                            addedExercise.setId(exerciseId);
                            database.collection("exercises")
                                    .document(exerciseId)
                                    .set(addedExercise)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d("FIRE_STORE", "DocumentSnapshot successfully written!");
                                        }
                                    });
                        }
                    });
        }
    }

    @Override
    public ArrayList<Workout> onGetRecentActivities(ArrayList<Workout> activities) {

        if (fragments.get(1).getClass() == RecentActivitiesFragment.class) {
            ((RecentActivitiesFragment) fragments.get(1)).setActivities(activities);
        }
        return activities;
    }
}
