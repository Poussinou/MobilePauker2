package com.daniel.mobilepauker2.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.dropbox.SyncDialog;
import com.daniel.mobilepauker2.model.ModelManager;
import com.daniel.mobilepauker2.model.SettingsManager;
import com.daniel.mobilepauker2.statistics.ChartAdapter;
import com.daniel.mobilepauker2.statistics.ChartAdapter.ChartAdapterCallback;
import com.daniel.mobilepauker2.utils.Constants;
import com.daniel.mobilepauker2.utils.ErrorReporter;
import com.daniel.mobilepauker2.utils.Log;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState;

import java.io.File;

import static com.daniel.mobilepauker2.model.ModelManager.LearningPhase.FILLING_USTM;
import static com.daniel.mobilepauker2.model.ModelManager.LearningPhase.SIMPLE_LEARNING;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.HIDE_TIMES;

/**
 * Created by Daniel on 24.02.2018.
 * Masterarbeit++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */

public class MainMenu extends AppCompatActivity {
    private static final int RQ_WRITE_EXT = 99;
    private final ModelManager modelManager = ModelManager.instance();
    private final PaukerManager paukerManager = PaukerManager.instance();
    private final SettingsManager settingsManager = SettingsManager.instance();
    private final Context context = this;
    private boolean firstStart = true;
    private MenuItem search;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ErrorReporter.instance().init(context);
        checkErrors();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        setContentView(R.layout.main_menu);

        if (!modelManager.isLessonSetup()) {
            modelManager.createNewLesson();
        }

        initButtons();
        initView();
        initChartList();
    }

    public void initButtons() {
        boolean hasCardsToLearn = modelManager.getUnlearnedBatchSize() != 0;
        boolean hasExpiredCards = modelManager.getExpiredCardsSize() != 0;

        findViewById(R.id.bLearnNewCard).setEnabled(hasCardsToLearn);
        findViewById(R.id.bLearnNewCard).setClickable(hasCardsToLearn);
        findViewById(R.id.tLearnNewCardDesc).setEnabled(hasCardsToLearn);

        findViewById(R.id.bRepeatExpiredCards).setEnabled(hasExpiredCards);
        findViewById(R.id.bRepeatExpiredCards).setClickable(hasExpiredCards);
        findViewById(R.id.tRepeatExpiredCardsDesc).setEnabled(hasExpiredCards);
    }

    private void initView() {
        invalidateOptionsMenu();

        String description = modelManager.getDescription();
        TextView descriptionView = findViewById(R.id.infoText);
        descriptionView.setText(description);
        if (!description.isEmpty()) {
            descriptionView.setMovementMethod(new ScrollingMovementMethod());
        }

        SlidingUpPanelLayout drawer = findViewById(R.id.drawerPanel);
        if (drawer != null) {
            drawer.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
                @Override
                public void onPanelSlide(View panel, float slideOffset) {

                }

                @Override
                public void onPanelStateChanged(View panel, PanelState previousState, PanelState newState) {
                    if (newState == PanelState.EXPANDED)
                        findViewById(R.id.drawerImage).setRotation(180);
                    if (newState == PanelState.COLLAPSED)
                        findViewById(R.id.drawerImage).setRotation(0);
                }
            });
            drawer.setPanelState(PanelState.COLLAPSED);
        }

        String title = getString(R.string.app_name);
        if (modelManager.isLessonNotNew()) {
            title = paukerManager.getReadableFileName();
        }
        setTitle(title);
    }

    private void initChartList() {
        final RecyclerView chartView = findViewById(R.id.chartListView);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context,
                LinearLayoutManager.HORIZONTAL, false);
        ChartAdapterCallback onClickListener = new ChartAdapterCallback() {
            @Override
            public void onClick(int position) {
                showBatchDetails(position);
            }
        };
        final ChartAdapter adapter = new ChartAdapter(context, onClickListener);
        chartView.setLayoutManager(layoutManager);
        chartView.setAdapter(adapter);
        chartView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        chartView.setScrollContainer(true);
        chartView.setNestedScrollingEnabled(true);
    }

    private void showBatchDetails(int index) {
        if (modelManager.getLessonSize() == 0) return;

        Intent browseIntent = new Intent(Intent.ACTION_SEARCH);
        browseIntent.setClass(context, SearchActivity.class);
        browseIntent.putExtra(SearchManager.QUERY, "");


        if ((index > 1 && modelManager.getBatchStatistics().get(index - 2).getBatchSize() == 0)
                || (index == 1 && modelManager.getUnlearnedBatchSize() == 0)) {
            return;
        }

        browseIntent.putExtra(Constants.STACK_INDEX, index);
        startActivity(browseIntent);
    }

    /**
     * Startet die Permissionanfrage
     */
    private void requestPermission() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                RQ_WRITE_EXT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem save = menu.findItem(R.id.mSaveFile);
        search = menu.findItem(R.id.mSearch);
        MenuItem open = menu.findItem(R.id.mOpenLesson);
        if (modelManager.isLessonNotNew()) {
            menu.setGroupEnabled(R.id.mGroup, true);
        } else {
            menu.setGroupEnabled(R.id.mGroup, false);
        }

        if (modelManager.getLessonSize() > 0) {
            search.setVisible(true);
            open.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        } else {
            search.setVisible(false);
            open.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        save.setVisible(paukerManager.isSaveRequired());

        if (search.isVisible()) {
            final SearchView searchView = (SearchView) search.getActionView();
            searchView.setIconifiedByDefault(false);
            searchView.setIconified(false);
            searchView.setQueryHint(getString(R.string.search_hint));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    Intent browseIntent = new Intent(Intent.ACTION_SEARCH);
                    browseIntent.setClass(context, SearchActivity.class);
                    browseIntent.putExtra(SearchManager.QUERY, query);
                    startActivity(browseIntent);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });
            searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) searchView.clearFocus();
                }
            });
        }

        return true;
    }

    @Override
    public void onResume() {
        Log.d("MainMenuActivity::onResume", "ENTRY");
        super.onResume();

        modelManager.resetLesson();

        if (search != null) {
            search.collapseActionView();
        }

        if (!firstStart) {
            initButtons();
            initView();
            initChartList();
        }
        firstStart = false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == RQ_WRITE_EXT) {
            if ((grantResults.length > 0) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openLesson();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CODE_SAVE_DIALOG_NORMAL) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(context, R.string.saving_success, Toast.LENGTH_SHORT).show();
                paukerManager.setSaveRequired(false);
                modelManager.showExpireToast(context);

                modelManager.addLesson(context);

                if (settingsManager.getBoolPreference(context, SettingsManager.Keys.AUTO_SYNC)) {
                    String accessToken = PreferenceManager.getDefaultSharedPreferences(context)
                            .getString(Constants.DROPBOX_ACCESS_TOKEN, null);
                    Intent intent = new Intent(context, SyncDialog.class);
                    intent.putExtra(SyncDialog.ACCESS_TOKEN, accessToken);
                    intent.putExtra(SyncDialog.FILES, new File(paukerManager.getFileAbsolutePath()));
                    startActivityForResult(intent, Constants.REQUEST_CODE_SYNC_DIALOG);
                }
            }
            invalidateOptionsMenu();
        } else if (requestCode == Constants.REQUEST_CODE_SYNC_DIALOG) {
            if (resultCode == RESULT_OK) {
                Log.d("SyncLesson", "Synchro erfolgreich");
            } else {
                Log.d("SyncLesson", "Synchro nicht erfolgreich");
                Toast.makeText(context, R.string.error_synchronizing, Toast.LENGTH_SHORT).show();
            }
        } /*else if (resultCode == RESULT_OK && requestCode == Constants.REQUEST_CODE_SYNC_DIALOG_BEFORE_OPEN) {
            startActivity(new Intent(context, LessonImportActivity.class));
        }*/ else if (requestCode == Constants.REQUEST_CODE_SAVE_DIALOG_NEW_LESSON) {
            if (resultCode == RESULT_OK) {
                createNewLesson();
            } else {
                Toast.makeText(context, R.string.saving_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void addNewCard(View view) {
        startActivity(new Intent(context, AddCardActivity.class));
    }

    public void learnNewCard(View view) {
        if (settingsManager.getBoolPreference(context, HIDE_TIMES)) {
            modelManager.setLearningPhase(context, SIMPLE_LEARNING);
        } else {
            modelManager.setLearningPhase(context, FILLING_USTM);
        }

        startActivity(new Intent(context, LearnCardsActivity.class));
    }

    public void repeatCards(View view) {
        modelManager.setLearningPhase(context, ModelManager.LearningPhase.REPEATING_LTM);
        Intent importActivity = new Intent(context, LearnCardsActivity.class);
        startActivity(importActivity);
    }

    /**
     * Speichert die Lektion.
     * @param requestCode Wird für onActivityResult benötigt
     */
    private void saveLesson(final int requestCode) {
        startActivityForResult(new Intent(context, SaveDialog.class), requestCode);
    }

    /**
     * Fragt, wenn notwendig, die Permission ab und zeigt davor einen passenden Infodialog an.
     */
    private void openLesson() {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.app_name)
                .setNegativeButton(R.string.not_now, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pref.edit().putBoolean("FirstTime", false).apply();
                        requestPermission();
                        dialog.dismiss();
                    }
                });
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                builder.setMessage(R.string.write_permission_rational_message);
            } else {
                if (pref.getBoolean("FirstTime", true)) {
                    builder.setMessage(R.string.write_permission_info_message);
                } else {
                    builder.setMessage(R.string.write_permission_rational_message)
                            .setPositiveButton(R.string.settings, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                                    intent.setData(uri);
                                    startActivity(intent);
                                    dialog.dismiss();
                                }
                            });
                }
            }
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            /*if (settingsManager.getBoolPreference(context, AUTO_SYNC)) {
                String accessToken = PreferenceManager.getDefaultSharedPreferences(context)
                        .getString(Constants.DROPBOX_ACCESS_TOKEN, null);
                if (accessToken != null) {
                    File[] files = paukerManager.listFiles(context);
                    Intent syncIntent = new Intent(context, SyncDialog.class);
                    syncIntent.putExtra(SyncDialog.ACCESS_TOKEN, accessToken);
                    syncIntent.putExtra(SyncDialog.FILES, files);
                    startActivityForResult(syncIntent, Constants.REQUEST_CODE_SYNC_DIALOG_BEFORE_OPEN);
                }
            } else {*/
            if (paukerManager.isSaveRequired()) {
                builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.lesson_not_saved_dialog_title)
                        .setMessage(R.string.lesson_not_saved_dialog_message)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(context, LessonImportActivity.class));
                                dialog.dismiss();
                            }
                        })
                        .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.create().show();
            } else
                startActivity(new Intent(context, LessonImportActivity.class));
            //}
        }
    }

    /**
     * Erstellt eine neue Lektion
     */
    private void createNewLesson() {
        paukerManager.setupNewApplicationLesson();
        paukerManager.setSaveRequired(false);
        initButtons();
        initChartList();
        initView();
        Toast.makeText(context, R.string.new_lession_created, Toast.LENGTH_SHORT).show();
    }

    public void mSaveFileClicked(@Nullable MenuItem ignored) {
        saveLesson(Constants.REQUEST_CODE_SAVE_DIALOG_NORMAL);
    }

    /**
     * Aktion des Menubuttons
     * @param ignored Nicht benötigt
     */
    public void mOpenLessonClicked(@Nullable MenuItem ignored) {
        openLesson();
    }

    public void mResetLessonClicked(MenuItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.reset_lesson_dialog_title)
                .setMessage(R.string.reset_lesson_dialog_info)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        modelManager.forgetAllCards();
                        paukerManager.setSaveRequired(true);
                        initButtons();
                        initChartList();
                        initView();
                        Toast.makeText(context, R.string.lektion_zurückgesetzt, Toast.LENGTH_SHORT).show();
                        dialog.cancel();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.create().show();
    }

    public void mNewLessonClicked(MenuItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.new_lesson_dialog_title)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (paukerManager.isSaveRequired()) {
                            saveLesson(Constants.REQUEST_CODE_SAVE_DIALOG_NEW_LESSON);
                        } else {
                            createNewLesson();
                        }
                        dialog.cancel();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.create().show();
    }

    public void mEditInfoTextClicked(@Nullable MenuItem ignored) {
        startActivity(new Intent(context, EditDescrptionActivity.class));
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.stay);
    }

    public void mSettingsClicked(MenuItem item) {
        startActivity(new Intent(context, SettingsActivity.class));
    }

    public void mOpenSearchClicked(MenuItem item) {
        SearchView searchView = (SearchView) item.getActionView();
        searchView.setIconified(false);
    }

    public void mFlipSidesClicked(MenuItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.reverse_sides_dialog_title)
                .setMessage(R.string.reverse_sides_dialog_info)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        modelManager.flipAllCards();
                        paukerManager.setSaveRequired(true);
                        initButtons();
                        initChartList();
                        initView();
                        Toast.makeText(context, R.string.flip_sides_complete, Toast.LENGTH_SHORT).show();
                        dialog.cancel();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.create().show();
    }

    private void checkErrors() {
        final ErrorReporter errorReporter = ErrorReporter.instance();
        if (errorReporter.isThereAnyErrorsToReport(this)) {
            AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);

            alt_bld.setMessage(getString(R.string.crash_report_message));
            alt_bld.setCancelable(false);
            alt_bld.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    errorReporter.CheckErrorAndSendMail(context);
                }
            });

            alt_bld.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    errorReporter.deleteErrorFiles(getApplicationContext());
                    dialog.cancel();
                }
            });

            AlertDialog alert = alt_bld.create();
            alert.setTitle(getString(R.string.crash_report_title));
            alert.setIcon(R.mipmap.ic_launcher);
            alert.show();
        }
    }
}