package com.example.contact_tracing;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.util.TypedValue;
import android.widget.Button;

import com.quickbirdstudios.surveykit.AnswerFormat;
import com.quickbirdstudios.surveykit.OrderedTask;
import com.quickbirdstudios.surveykit.StepIdentifier;
import com.quickbirdstudios.surveykit.SurveyTheme;
import com.quickbirdstudios.surveykit.TaskIdentifier;
import com.quickbirdstudios.surveykit.TextChoice;
import com.quickbirdstudios.surveykit.steps.InstructionStep;
import com.quickbirdstudios.surveykit.steps.QuestionStep;
import com.quickbirdstudios.surveykit.steps.Step;
import com.quickbirdstudios.surveykit.survey.SurveyView;

import java.util.ArrayList;
import java.util.List;

public class DisplaySurveyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_survey);
        List<TextChoice> symptomsChoices = new ArrayList<>();
        symptomsChoices.add(new TextChoice("Sore Throat", "5"));
        AnswerFormat firstQuestionAnswerFormat = new AnswerFormat.MultipleChoiceAnswerFormat(symptomsChoices, symptomsChoices);
        QuestionStep firstQuestion = new QuestionStep("Do you have any of these symptoms?",
                "Select all that apply","next", firstQuestionAnswerFormat, false, new StepIdentifier("1"));
        List<Step> step = new ArrayList<>();
        step.add(firstQuestion);
        OrderedTask task =  new OrderedTask(step, new TaskIdentifier("1"));
        SurveyView survey = findViewById(R.id.survey_view);
        SurveyTheme configuration = new SurveyTheme(
                ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.teal),
                ContextCompat.getColor(this, R.color.teal)
        );
        survey.start(task, configuration);
    }


}
