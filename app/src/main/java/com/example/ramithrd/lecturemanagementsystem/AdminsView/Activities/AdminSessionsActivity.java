package com.example.ramithrd.lecturemanagementsystem.AdminsView.Activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.example.ramithrd.lecturemanagementsystem.AdminsView.Adapters.AdminAdapter;
import com.example.ramithrd.lecturemanagementsystem.LecturerView.Adapters.LecSessionsAdapter;
import com.example.ramithrd.lecturemanagementsystem.Model.Session;
import com.example.ramithrd.lecturemanagementsystem.R;

import java.util.ArrayList;

public class AdminSessionsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminAdapter sessionsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_sessions);

        ArrayList<Session> sessionsList = getIntent().getExtras().getParcelableArrayList("sessionsList");

        getSupportActionBar().setTitle("Lectures on "+sessionsList.get(0).getLec_date());

        recyclerView = (RecyclerView) findViewById(R.id.adm_sessions_recycler);
        recyclerView.setNestedScrollingEnabled(false);
        sessionsAdapter = new AdminAdapter(sessionsList);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);

        recyclerView.setAdapter(sessionsAdapter);
    }
}
