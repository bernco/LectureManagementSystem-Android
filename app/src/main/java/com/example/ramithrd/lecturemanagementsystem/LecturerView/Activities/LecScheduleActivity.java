package com.example.ramithrd.lecturemanagementsystem.LecturerView.Activities;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;


import com.borax12.materialdaterangepicker.time.RadialPickerLayout;
import com.borax12.materialdaterangepicker.time.TimePickerDialog;
import com.example.ramithrd.lecturemanagementsystem.GlobalClass;
import com.example.ramithrd.lecturemanagementsystem.Helpers.NetworkCheck;
import com.example.ramithrd.lecturemanagementsystem.LecturerView.Interfaces.LectureSessionService;
import com.example.ramithrd.lecturemanagementsystem.Model.Batch;
import com.example.ramithrd.lecturemanagementsystem.Model.LectureHall;
import com.example.ramithrd.lecturemanagementsystem.Model.LectureSession;
import com.example.ramithrd.lecturemanagementsystem.Model.Module;
import com.example.ramithrd.lecturemanagementsystem.Model.Programme;
import com.example.ramithrd.lecturemanagementsystem.Model.Session;
import com.example.ramithrd.lecturemanagementsystem.Model.University;
import com.example.ramithrd.lecturemanagementsystem.R;
import com.jaredrummler.materialspinner.MaterialSpinner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LecScheduleActivity extends AppCompatActivity implements TimePickerDialog.OnTimeSetListener {

    private ImageButton pickLecTime;
    private ImageButton pickLecDate;

    private TextInputLayout startTimeLayout;
    private TextInputLayout endTimeLayout;
    private TextInputLayout dateLayout;

    private EditText selectedStartTimeTxt;
    private EditText selectedEndTimeTxt;
    private EditText selectedDateTxt;

    private MaterialSpinner universitiesSpinner;
    private MaterialSpinner programmesSpinner;
    private MaterialSpinner batchesSpinner;
    private MaterialSpinner modulesSpinner;
    private MaterialSpinner lectureHallsSpinner;

    private List<String> universitiesList;
    private List<String> progsList;
    private List<String> batchesList;
    private List<String> moduleList;
    private List<String> lectureHallsList;

    private List<LectureHall> lectureHallIds;

    private HashMap<String, String> uniMap;
    private HashMap<String, String> programmeMap;
    private HashMap<String, String> moduleMap;

    private int mYear, mMonth, mDay;

    private GlobalClass globalClass;
    private String lecturerID = "";

    private String selectedUniversityId = "";
    private String selectedProgrammeId = "";
    private String selectedModuleId = "";
    private String selectedBatchId = "";
    private String selectedLecHallId = "";
    private String lecFaculty = "";
    private String selectedLecDate = "";
    private String selectedLecStartTime = "";
    private String selectedLecEndTime = "";


    private ProgressDialog mLoadDetailsDialog;
    private Button addLectureSession;

    private LectureSessionService lecSessionService;

    private String taskMode = "";
    private Session sessionToUpdate;

    private LinearLayout lecScheduleContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lec_schedule);

        lecScheduleContainer = (LinearLayout) findViewById(R.id.lecScheduleContainer);

        registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {

                int status = NetworkCheck.getConnectivityStatusString(context);

                if(status == 0){

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Window window = getWindow();
                        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                        window.setStatusBarColor(Color.RED);
                    }

                    Snackbar.make(lecScheduleContainer, "Device Offline, Some Features Will Not Function Properly!", Snackbar.LENGTH_LONG)
                            .setAction("GO ONLINE", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent = new Intent(Settings.ACTION_SETTINGS);
                                    startActivity(intent);
                                }
                            })
                            .setActionTextColor(getResources()
                                    .getColor(android.R.color.holo_red_light))
                            .show();
                }else{
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Window window = getWindow();
                        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                        window.setStatusBarColor(Color.parseColor("#00796B"));
                    }
                }

            }}, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

        Bundle extras = getIntent().getExtras();
        if (extras != null) {

            sessionToUpdate = extras.getParcelable("sessionToUpdate");
            taskMode = "update";

        }else{
            taskMode = "add";
        }

        final String ENDPOINT_URL  = getString(R.string.lecturer_service_url);

        mLoadDetailsDialog = new ProgressDialog(this);
        addLectureSession = (Button) findViewById(R.id.addLectureSessionBtn);


        addLectureSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!validateFields()){

                    switch (taskMode){

                        case "add":{
                            addLecture();
                            break;
                        }
                        case "update":{
                            updateLecture(sessionToUpdate);
                            break;
                        }

                    }
                }
            }
        });

        globalClass = ((GlobalClass) getApplicationContext());
        lecturerID =  globalClass.getUserInfo().getUserId();

        startTimeLayout = (TextInputLayout) findViewById(R.id.schedule_start_time_layout);
        endTimeLayout = (TextInputLayout) findViewById(R.id.schedule_end_time_layout);
        dateLayout = (TextInputLayout) findViewById(R.id.schedule_date_layout);

        selectedStartTimeTxt = (EditText) findViewById(R.id.schedule_picked_layout);
        selectedEndTimeTxt = (EditText) findViewById(R.id.schedule_end_time);
        selectedDateTxt = (EditText) findViewById(R.id.schedule_picked_date);

        uniMap = new HashMap<>();
        programmeMap = new HashMap<>();
        moduleMap = new HashMap<>();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ENDPOINT_URL)
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        lecSessionService = retrofit.create(LectureSessionService.class);

        universitiesSpinner = (MaterialSpinner) findViewById(R.id.unis_spinner);
        universitiesSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, Object item) {

                String uniName = item.toString();
                selectedUniversityId = uniMap.get(uniName);
                System.out.println("I2K University :"+selectedUniversityId);

                Call<List<Programme>> programmeRequest = lecSessionService.getProgrammes(lecturerID,selectedUniversityId);
                programmeRequest.enqueue(new Callback<List<Programme>>() {
                    @Override
                    public void onResponse(Call<List<Programme>> call, Response<List<Programme>> response) {
                        List<Programme> programmesList = response.body();
                        progsList = new ArrayList<String>();
                        progsList.add("Select Programme");

                        for(Programme prog : programmesList){

                            progsList.add(prog.getName());
                            programmeMap.put(prog.getName(),prog.getProgrammeId());

                        }

                        programmesSpinner.setItems(progsList);
                    }

                    @Override
                    public void onFailure(Call<List<Programme>> call, Throwable t) {

                    }
                });
            }
        });
        programmesSpinner = (MaterialSpinner) findViewById(R.id.programmes_spinner);
        programmesSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, Object item) {

                String programName = item.toString();
                selectedProgrammeId = programmeMap.get(programName);
                System.out.println("I2K Programme :"+selectedProgrammeId);
                Call<List<Batch>> batchRequest = lecSessionService.getBatches(lecturerID,selectedProgrammeId);
                batchRequest.enqueue(new Callback<List<Batch>>() {
                    @Override
                    public void onResponse(Call<List<Batch>> call, Response<List<Batch>> response) {
                        List<Batch> batchessList = response.body();

                        batchesList = new ArrayList<String>();
                        batchesList.add("Select Batch");

                        for(Batch batch: batchessList){

                            batchesList.add(batch.getBatchId());

                        }


                        batchesSpinner.setItems(batchesList);
                    }

                    @Override
                    public void onFailure(Call<List<Batch>> call, Throwable t) {

                    }
                });

                Call<List<Module>> modulesRequest = lecSessionService.getModules(lecturerID,selectedProgrammeId);
                modulesRequest.enqueue(new Callback<List<Module>>() {
                    @Override
                    public void onResponse(Call<List<Module>> call, Response<List<Module>> response) {
                        List<Module> modulesList = response.body();

                        moduleList = new ArrayList<String>();
                        moduleList.add("Select Module");

                        for(Module module: modulesList){

                            moduleList.add(module.getName());
                            moduleMap.put(module.getName(),module.getModuleId());
                        }


                        modulesSpinner.setItems(moduleList);
                    }

                    @Override
                    public void onFailure(Call<List<Module>> call, Throwable t) {

                    }
                });

            }
        });
        batchesSpinner = (MaterialSpinner) findViewById(R.id.batch_spinner);
        batchesSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, Object item) {
                selectedBatchId = item.toString();
                System.out.println("I2K Batch :"+selectedBatchId);
            }
        });
        modulesSpinner = (MaterialSpinner) findViewById(R.id.module_spinner);
        modulesSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, Object item) {
                String moduleName = item.toString();
                selectedModuleId = moduleMap.get(moduleName);
                System.out.println("I2K Module :"+selectedModuleId);
            }
        });
        lectureHallsSpinner = (MaterialSpinner) findViewById(R.id.hall_spinner);
        lectureHallsSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, Object item) {
                selectedLecHallId = item.toString();
                lecFaculty = lectureHallIds.get(position).getFaculty();
                System.out.println("I2K Lec-Hall :"+selectedLecHallId);
            }
        });


        Call<List<University>> uniRequest =  lecSessionService.getUniversities(lecturerID);
        uniRequest.enqueue(new Callback<List<University>>() {
            @Override
            public void onResponse(Call<List<University>> call, Response<List<University>> response) {

                List<University> universityList = response.body();

                universitiesList = new ArrayList<String>();
                universitiesList.add("Select University");

                for(University uni : universityList){

                    universitiesList.add(uni.getName());
                    uniMap.put(uni.getName(),uni.getUniversityId());
                }


                universitiesSpinner.setItems(universitiesList);
            }

            @Override
            public void onFailure(Call<List<University>> call, Throwable t) {

            }
        });

        Call<List<LectureHall>> lecHallRequest = lecSessionService.getLectureHalls();
        lecHallRequest.enqueue(new Callback<List<LectureHall>>() {
            @Override
            public void onResponse(Call<List<LectureHall>> call, Response<List<LectureHall>> response) {
                System.out.println("SUCCESS");

                lectureHallIds = response.body();

                lectureHallsList = new ArrayList<String>();

                for(LectureHall lec : lectureHallIds){

                    //get faculty
                    lectureHallsList.add(lec.getLectureHallName());

                }


                lectureHallsSpinner.setItems(lectureHallsList);

            }

            @Override
            public void onFailure(Call<List<LectureHall>> call, Throwable t) {
                System.out.println("FAILURE: "+t.getMessage());
            }
        });

        pickLecTime = (ImageButton) findViewById(R.id.lecTimeBtn);
        pickLecTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Calendar now = Calendar.getInstance();
                TimePickerDialog timePicker = TimePickerDialog.newInstance(
                        LecScheduleActivity.this,
                        now.get(Calendar.HOUR_OF_DAY),
                        now.get(Calendar.MINUTE),
                        false
                );
                timePicker.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        Log.d("TimePicker", "Dialog was cancelled");
                    }
                });

                timePicker.show(getFragmentManager(), "BTimepickerdialog");

            }
        });

        pickLecDate = (ImageButton) findViewById(R.id.lecDateBtn);
        pickLecDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Calendar calendar = Calendar.getInstance();
                mYear = calendar.get(Calendar.YEAR);
                mMonth = calendar.get(Calendar.MONTH);
                mDay = calendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePicker = new DatePickerDialog(LecScheduleActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

                        selectedDateTxt.setText(year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);
                        selectedLecDate = selectedDateTxt.getText().toString();

                    }
                }, mYear, mMonth, mDay);

                //sets today's date as minimum date and all the past dates are disabled
                datePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
                datePicker.setCancelable(false);
                datePicker.show();

            }
        });


        switch (taskMode){

            case "add":{
                addLectureSession.setText("Add Schedule");
                break;
            }
            case "update":{
                addLectureSession.setText("Update Schedule");
                setSessionDetails();
                break;
            }


        }
    }

    private void addLecture() {

        mLoadDetailsDialog.setMessage("Adding Lecture ...");
        mLoadDetailsDialog.setCancelable(false);
        mLoadDetailsDialog.show();

        String lecDateString = selectedLecDate+" "+selectedLecStartTime;
        String lecStartString = selectedLecDate+" "+selectedLecStartTime;
        String lecEndString = selectedLecDate+" "+selectedLecEndTime;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        Date lecDateTime = new Date();
        Date startDateTime = new Date();
        Date endDateTime = new Date();
        try {
            lecDateTime = dateFormat.parse(lecDateString);
            startDateTime = dateFormat.parse(lecStartString);
            endDateTime = dateFormat.parse(lecEndString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        final long lectureDate = lecDateTime.getTime();
        final String lecDate = "/Date("+lectureDate+")/";

        final long lectureStartDateTime = startDateTime.getTime();
        final String lecStartDate = "/Date("+lectureStartDateTime+")/";

        final long lectureEndDateTime = endDateTime.getTime();
        final String lecEndDate = "/Date("+lectureEndDateTime+")/";

        LectureSession lecSession = new LectureSession();
        lecSession.setUserId(lecturerID);
        lecSession.setLecturerId(lecturerID);
        lecSession.setBatchId(selectedBatchId);
        lecSession.setModuleId(selectedModuleId);
        lecSession.setUniversityId(selectedUniversityId);
        lecSession.setProgrammeId(selectedProgrammeId);
        lecSession.setLectureHallName(selectedLecHallId);
        lecSession.setFaculty(lecFaculty);
        lecSession.setSessionStartTime(lecStartDate);
        lecSession.setSessionEndTime(lecEndDate);
        lecSession.setSessionDate(lecDate);

        System.out.println("DATE : "+lecDate+"\n LexStart :"+lecStartDate+"\n LecEnd : "+lecEndDate);

        //TODO response returns an int, if int == 0 ---> error __ else if int > 0 ----> success
        Call<Integer> addLectureSession = lecSessionService.AddSession(lecSession);
        addLectureSession.enqueue(new Callback<Integer>() {
            @Override
            public void onResponse(Call<Integer> call, Response<Integer> response) {

                mLoadDetailsDialog.hide();

                Snackbar snackbar = Snackbar
                        .make(lecScheduleContainer, "New Lecture Scheduled Successfully!", Snackbar.LENGTH_LONG);

                snackbar.show();

                Integer b = response.body();
                System.out.println("REAL val"+b);
                System.out.println("Success");

                //show success dialog
            }

            @Override
            public void onFailure(Call<Integer> call, Throwable t) {
                mLoadDetailsDialog.hide();
                Snackbar snackbar = Snackbar
                        .make(lecScheduleContainer, "Error Occured While Adding New LEcture, Please Try Again!", Snackbar.LENGTH_LONG);

                snackbar.show();
                System.out.println("Failure "+t.getMessage());
            }
        });

    }

    private void updateLecture(Session session){

        mLoadDetailsDialog.setMessage("Updating Lecture ...");
        mLoadDetailsDialog.setCancelable(false);
        mLoadDetailsDialog.show();

        String lecDateString = selectedLecDate+" "+selectedLecStartTime;
        String lecStartString = selectedLecDate+" "+selectedLecStartTime;
        String lecEndString = selectedLecDate+" "+selectedLecEndTime;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        Date lecDateTime = new Date();
        Date startDateTime = new Date();
        Date endDateTime = new Date();
        try {
            lecDateTime = dateFormat.parse(lecDateString);
            startDateTime = dateFormat.parse(lecStartString);
            endDateTime = dateFormat.parse(lecEndString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        final long lectureDate = lecDateTime.getTime();
        final String lecDate = "/Date("+lectureDate+")/";

        final long lectureStartDateTime = startDateTime.getTime();
        final String lecStartDate = "/Date("+lectureStartDateTime+")/";

        final long lectureEndDateTime = endDateTime.getTime();
        final String lecEndDate = "/Date("+lectureEndDateTime+")/";

        LectureSession lecSession = new LectureSession();
        lecSession.setSessionId(session.getSession_Id());
        lecSession.setUserId(lecturerID);
        lecSession.setLecturerId(lecturerID);
        lecSession.setBatchId(selectedBatchId);
        lecSession.setModuleId(selectedModuleId);
        lecSession.setUniversityId(selectedUniversityId);
        lecSession.setProgrammeId(selectedProgrammeId);
        lecSession.setLectureHallName(selectedLecHallId);
        lecSession.setFaculty(lecFaculty);
        lecSession.setSessionStartTime(lecStartDate);
        lecSession.setSessionEndTime(lecEndDate);
        lecSession.setSessionDate(lecDate);

        System.out.println("Session ID : "+session.getSession_Id());

        Call<Boolean> updateSession = lecSessionService.updateSession(lecSession);
        updateSession.enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {


                Boolean b = response.body();
                System.out.println("REAL val"+b);
                System.out.println("Success");
            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable t) {
                System.out.println("Failure "+t.getMessage());

            }
        });

        Call<Boolean> updateSessionTimes = lecSessionService.updateSessionDateTime(lecSession);
        updateSessionTimes.enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {

                mLoadDetailsDialog.hide();

                Snackbar snackbar = Snackbar
                        .make(lecScheduleContainer, "Lecture Session Successfully Updated!", Snackbar.LENGTH_LONG);

                snackbar.show();

                Boolean b = response.body();
                System.out.println("REAL val"+b);
                System.out.println("Success : times updated");
            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable t) {
                mLoadDetailsDialog.hide();
                Snackbar snackbar = Snackbar
                        .make(lecScheduleContainer, "Error Occured While Updating Session, Please Try Again!", Snackbar.LENGTH_LONG);

                snackbar.show();
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();

    }


    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute, int hourOfDayEnd, int minuteEnd) {

        String hourString = hourOfDay < 10 ? "0"+hourOfDay : ""+hourOfDay;
        String minuteString = minute < 10 ? "0"+minute : ""+minute;
        String hourStringEnd = hourOfDayEnd < 10 ? "0"+hourOfDayEnd : ""+hourOfDayEnd;
        String minuteStringEnd = minuteEnd < 10 ? "0"+minuteEnd : ""+minuteEnd;

        String startTime = hourString+":"+minuteString+":00";
        String endTime = hourStringEnd+":"+minuteStringEnd+":00";

        selectedLecStartTime = startTime;
        selectedLecEndTime = endTime;

        selectedStartTimeTxt.setText(startTime);
        selectedEndTimeTxt.setText(endTime);
    }

    private static OkHttpClient getOkHttpClient(){
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.NONE);
        OkHttpClient okClient = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
        return okClient;
    }

    private void setSessionDetails(){

        //remove the date from start and end times
        String startTimefull = sessionToUpdate.getLec_start_time();
        String endTimeFull = sessionToUpdate.getLec_end_time();

        String[] startTimeArr = startTimefull.split(" ");
        String[] endTimeArr = endTimeFull.split(" ");

        selectedStartTimeTxt.setText(startTimeArr[1]);
        selectedEndTimeTxt.setText(endTimeArr[1]);
        selectedDateTxt.setText(sessionToUpdate.getLec_date());

    }

    private boolean validateFields(){

        boolean errorOccurred = false;

        if(TextUtils.isEmpty(selectedUniversityId)){
            errorOccurred = true;
            universitiesSpinner.setError("Select a University");
        }else{
            errorOccurred = false;
            universitiesSpinner.setError(null);
        }

        if(TextUtils.isEmpty(selectedProgrammeId)){
            errorOccurred = true;
            programmesSpinner.setError("Select a Programme");
        }else{
            errorOccurred = false;
            programmesSpinner.setError(null);
        }

        if(TextUtils.isEmpty(selectedModuleId)){
            errorOccurred = true;
            modulesSpinner.setError("Select a Module");
        }else{
            errorOccurred = false;
            modulesSpinner.setError(null);
        }

        if(TextUtils.isEmpty(selectedBatchId)){
            errorOccurred = true;
            batchesSpinner.setError("Select a Batch");
        }else{
            errorOccurred = false;
            batchesSpinner.setError(null);
        }

        if(TextUtils.isEmpty(selectedLecHallId)){
            errorOccurred = true;
            lectureHallsSpinner.setError("Select a Lecture Hall");
        }else{
            errorOccurred = false;
            lectureHallsSpinner.setError(null);
        }

        if(TextUtils.isEmpty(selectedLecDate)){
            errorOccurred = true;
            dateLayout.setErrorEnabled(true);
            dateLayout.setError("Select Lecture Date");
        }else{
            errorOccurred = false;
            dateLayout.setErrorEnabled(false);
        }

        if(TextUtils.isEmpty(selectedLecStartTime)){
            errorOccurred = true;
            startTimeLayout.setErrorEnabled(true);
            startTimeLayout.setError("Select Lecture Start Time");
        }else{
            errorOccurred = false;
            startTimeLayout.setErrorEnabled(false);
        }

        if(TextUtils.isEmpty(selectedLecEndTime)){
            errorOccurred = true;
            endTimeLayout.setErrorEnabled(true);
            endTimeLayout.setError("Select Lecture End Time");
        }else{
            errorOccurred = false;
            endTimeLayout.setErrorEnabled(false);
        }

        return errorOccurred;
    }
}
