package com.covid.contact_tracing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.covid.contact_tracing.R;
import com.quickbirdstudios.surveykit.AnswerFormat;
import com.quickbirdstudios.surveykit.FinishReason;
import com.quickbirdstudios.surveykit.OrderedTask;
import com.quickbirdstudios.surveykit.StepIdentifier;
import com.quickbirdstudios.surveykit.SurveyTheme;
import com.quickbirdstudios.surveykit.TaskIdentifier;
import com.quickbirdstudios.surveykit.TextChoice;
import com.quickbirdstudios.surveykit.result.StepResult;
import com.quickbirdstudios.surveykit.result.TaskResult;
import com.quickbirdstudios.surveykit.steps.QuestionStep;
import com.quickbirdstudios.surveykit.steps.Step;
import com.quickbirdstudios.surveykit.survey.SurveyView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.Intrinsics;

public class DisplaySurveyActivity extends AppCompatActivity {
    static final String FEVER = "Fever, chills or sweating";
    static final String BREATHING = "Difficulty Breathing";
    static final String COUGH = "New or worsening cough";
    static final String BODY_ACHES = "Whole body aches";
    static final String FATIGUE = "Fatigue";
    static final String LOSS_OF_APPETITE = "Severe loss of appetite";
    static final String NONE = "None of the above";
    static final String CLOSE_CONTACT = "Have close contact with someone who has COVID-19 (Within 6 feet)";
    static final String TRAVEL_INTERNATIONALLY = "Travel internationally";
    static final String LIVE_IN_AREA = "Live in or visit a place where COVID-19 is widespread";
    private Context context;
    private Integer atRisk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_survey);
        context = this;
        List<Step> step = new ArrayList<>();
        step.add(createFirstQuestion());
        step.add(createSecondQuestion());
        step.add(createThirdQuestion());
        OrderedTask task = new OrderedTask(step, new TaskIdentifier("1"));
        SurveyView survey = findViewById(R.id.survey_view);


        SurveyTheme configuration = new SurveyTheme(
                ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.teal),
                ContextCompat.getColor(this, R.color.teal)
        );
        survey.start(task, configuration);
        survey.setOnSurveyFinish((Function2) new Function2() {
            // $FF: synthetic method
            // $FF: bridge method
            public Object invoke(Object var1, Object var2) {
                this.invoke((TaskResult) var1, (FinishReason) var2);
                return Unit.INSTANCE;
            }

            final void invoke(@NotNull TaskResult taskResult, @NotNull FinishReason reason) {
                Intrinsics.checkParameterIsNotNull(taskResult, "taskResult");
                Intrinsics.checkParameterIsNotNull(reason, "reason");
                if (reason == FinishReason.Completed) {
                    Iterable tasksIterable = (Iterable) taskResult.getResults();
                    Iterator tasksIterator = tasksIterable.iterator();
                    double riskPercent = 0.0;
                    while (tasksIterator.hasNext()) {
                        Object task = tasksIterator.next();
                        StepResult stepResult = (StepResult) task;
                        String stepId = stepResult.getId().getId();
                        // Do stuff with the survey data
                        String answerWeights = stepResult.getResults().iterator().next().getStringIdentifier();
                        String[] weights = answerWeights.split(",");
                        double totalWeight = 0.0;
                        for (String weight : weights) {
                            totalWeight += Double.parseDouble(weight);
                        }
                        if (stepId.equals("1")) {
                            riskPercent += 40 * totalWeight;
                        } else if (stepId.equals("2")) {
                            riskPercent += 30 * totalWeight;
                        } else {
                            riskPercent += 30 * totalWeight;
                        }
                    }
                    atRisk = (Integer) (int) Math.round(riskPercent);

                }
                // Resume DisplayStatisticsActivity
                Intent returnToStatisticsActivity = new Intent();
                returnToStatisticsActivity.putExtra("surveyRiskPercentage", atRisk);
                setResult(Activity.RESULT_OK, returnToStatisticsActivity);
                finish();
            }
        });


    }

    private QuestionStep createFirstQuestion() {
        List<TextChoice> symptomsChoices = new ArrayList<>();
        List<TextChoice> empty = new ArrayList<>();
        HashMap<String, String> weights = new HashMap<String, String>() {{
            put(FEVER, "0.267");
            put(BREATHING, "0.105");
            put(COUGH, "0.209");
            put(BODY_ACHES, "0.067");
            put(FATIGUE, "0.167");
            put(LOSS_OF_APPETITE, "0.182");
            put(NONE, "0.0");
        }};
        ArrayList<String> symptoms = new ArrayList<>(Arrays.asList(FEVER, BREATHING, COUGH, BODY_ACHES, FATIGUE, LOSS_OF_APPETITE, NONE));
        for (String symptom : symptoms) {
            String weight = weights.get(symptom);
            symptomsChoices.add(new TextChoice(symptom, Objects.requireNonNull(weight)));
        }

        AnswerFormat firstQuestionAnswerFormat = new AnswerFormat.MultipleChoiceAnswerFormat(symptomsChoices, empty);
        return new QuestionStep("Do you have any of these symptoms?",
                "Select all that apply", "next", firstQuestionAnswerFormat, false, new StepIdentifier("1"));
    }

    private QuestionStep createSecondQuestion() {
        List<TextChoice> choices = new ArrayList<>(Arrays.asList(new TextChoice("Yes", "1"), new TextChoice("No", "0")));
        List<TextChoice> empty = new ArrayList<>();
        AnswerFormat secondQuestionAnswerFormat = new AnswerFormat.MultipleChoiceAnswerFormat(choices, empty);
        return new QuestionStep("Do you work or volunteer in a healthcare setting",
                "Such as a hospital, nursing home, long-term care facility, or first-responder service", "next", secondQuestionAnswerFormat, false, new StepIdentifier("2"));
    }

    private QuestionStep createThirdQuestion() {
        List<TextChoice> options = new ArrayList<>();
        HashMap<String, String> weights = new HashMap<String, String>() {{
            put(CLOSE_CONTACT, "0.33");
            put(TRAVEL_INTERNATIONALLY, "0.33");
            put(LIVE_IN_AREA, "0.33");
            put(NONE, "0");
        }};
        ArrayList<String> choices = new ArrayList<>(Arrays.asList(CLOSE_CONTACT, TRAVEL_INTERNATIONALLY, LIVE_IN_AREA, NONE));
        List<TextChoice> empty = new ArrayList<>();
        for (String option : choices) {
            options.add(new TextChoice(option, Objects.requireNonNull(weights.get(option))));
        }
        AnswerFormat secondQuestionAnswerFormat = new AnswerFormat.MultipleChoiceAnswerFormat(options, empty);
        return new QuestionStep("In the last 2 weeks, did you:",
                "Select all the apply", "next", secondQuestionAnswerFormat, false, new StepIdentifier("3"));
    }
}
