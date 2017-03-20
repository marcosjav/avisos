package com.xskin.avisos;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.NotificationManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.util.Printer;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.security.PrivateKey;

public class AvisosActivity extends AppCompatActivity {

    private ListView mListView;
    private AvisosDBAdapter mDbAdapter;
    private AvisosSimpleCursorAdapter mCursorAdapter;
    private NotificationManager notificacion;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avisos);
        mListView = (ListView) findViewById(R.id.avisos_list_view);
        findViewById(R.id.avisos_list_view);
        mListView.setDivider(null);
        mDbAdapter = new AvisosDBAdapter(this);
        mDbAdapter.open();

        if (savedInstanceState==null){
            mDbAdapter.deleteAllReminders();

            mDbAdapter.createReminder("si anda wacho", false);
            mDbAdapter.createReminder("metele nieri", false);
            mDbAdapter.createReminder("este es importante", true);
        }
        Cursor cursor = mDbAdapter.fetchAllReminders();

        //desde las columnas definidas en la base de datos
        String[] from = new String[]{
                AvisosDBAdapter.COL_CONTENT
        };

        //a la id de views en el layout
        int[] to = new int[]{
                R.id.row_text
        };

        mCursorAdapter = new AvisosSimpleCursorAdapter(
                //context
                AvisosActivity.this,
                //el layout de la fila
                R.layout.avisos_row,
                //cursor
                cursor,
                //desde columnas definidas en la base de datos
                from,
                //a las ids de views en el layout
                to,
                //flag - no usado
                0);

        //el cursorAdapter (controller) está ahora actualizando la listView (view)
        //con datos desde la base de datos (modelo)
        mListView.setAdapter(mCursorAdapter);

        // cuando pulsamos un item individual en la  listview
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, final int masterListPosition, long id) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AvisosActivity.this);
                ListView modeListView = new ListView(AvisosActivity.this);
                String[] modes = new String[] { "Editar Aviso", "Borrar Aviso" };
                ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(AvisosActivity.this,
                        android.R.layout.simple_list_item_1, android.R.id.text1, modes);
                modeListView.setAdapter(modeAdapter);
                builder.setView(modeListView);
                final Dialog dialog = builder.create();
                dialog.show();
                modeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        //editar aviso
                        if (position == 0) {
                            int nId = getIdFromPosition(masterListPosition);
                            Aviso aviso = mDbAdapter.fetchReminderById(nId);
                            fireCustomDialog(aviso);
                            //borrar aviso
                        } else {
                            mDbAdapter.deleteReminderById(getIdFromPosition(masterListPosition));
                            mCursorAdapter.changeCursor(mDbAdapter.fetchAllReminders());
                        }
                        dialog.dismiss();
                    }
                });
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {

            mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
            mListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

                @Override
                public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                }

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(R.menu.cam_menu, menu);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.menu_item_delete_aviso:
                            for (int nC = mCursorAdapter.getCount() - 1; nC >= 0; nC--) {
                                if (mListView.isItemChecked(nC)) {
                                    mDbAdapter.deleteReminderById(getIdFromPosition(nC));
                                }
                            }
                            mode.finish();
                            mCursorAdapter.changeCursor(mDbAdapter.fetchAllReminders());
                            return true;
                    }
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                }
            });

        }

        notificacion = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private void fireCustomDialog(final Aviso aviso) {
        // custom dialog
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_custom);

        TextView titleView = (TextView) dialog.findViewById(R.id.custom_title);
        final EditText editCustom = (EditText) dialog.findViewById(R.id.custom_edit_reminder);
        Button commitButton = (Button) dialog.findViewById(R.id.custom_button_commit);

        final CheckBox checkBox = (CheckBox) dialog.findViewById(R.id.custom_check_box);
        LinearLayout rootLayout = (LinearLayout) dialog.findViewById(R.id.custom_root_layout);
        final boolean isEditOperation = (aviso != null);

        //esto es para un edit
        if (isEditOperation) {
            titleView.setText("Editar Aviso");
            checkBox.setChecked(aviso.getImportant() == 1);
            editCustom.setText(aviso.getContent());
            rootLayout.setBackgroundColor(getResources().getColor(R.color.azul_neutro));
        }

        commitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String reminderText = editCustom.getText().toString();
                if (isEditOperation) {
                    Aviso reminderEdited = new Aviso(aviso.getId(),
                            reminderText, checkBox.isChecked() ? 1 : 0);
                    mDbAdapter.updateReminder(reminderEdited);
                    //esto es para nuevo aviso
                } else {
                    mDbAdapter.createReminder(reminderText, checkBox.isChecked());
                }
                mCursorAdapter.changeCursor(mDbAdapter.fetchAllReminders());
                dialog.dismiss();
            }
        });

        Button buttonCancel = (Button) dialog.findViewById(R.id.custom_button_cancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private int getIdFromPosition(int nC) {
        return (int)mCursorAdapter.getItemId(nC);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_avisos, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_nuevo:
                fireCustomDialog(null);
                return true;

            case R.id.action_notificacion:
                CreateNotification();
                return true;

            case R.id.action_salir:
                finish();
                return true;

            default:
                return false;
        }

    }

    private void CreateNotification() {
        // notificacion dialog
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_notification);

        TextView titleView = (TextView) dialog.findViewById(R.id.custom_title);
        final EditText editCustom = (EditText) dialog.findViewById(R.id.not_reminder);
        final EditText editContent = (EditText) dialog.findViewById(R.id.not_content);
        Button commitButton = (Button) dialog.findViewById(R.id.not_btn_enviar);
        LinearLayout rootLayout = (LinearLayout) dialog.findViewById(R.id.custom_root_layout);
        // boton enviar y cancelar, llama al metodo que setea la notificacion.
        commitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String reminderText = editCustom.getText().toString();
                String ContentText = editContent.getText().toString();
                //metodo de notificacion con 5 paramentros se puede poner mas cosas si es necesario.
                notificacion(1, R.mipmap.ic_launcher, "Nombre APP", ContentText, reminderText);
                dialog.dismiss();
            }
        });

        Button buttonCancel = (Button) dialog.findViewById(R.id.not_btn_cancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void notificacion(int id, int iconId, String titulo, String contenido, String content){
        NotificationCompat.Builder builder=
                (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                        .setSmallIcon(iconId)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                                R.mipmap.ic_launcher))

                        .setColor(getResources().getColor(R.color.colorAccent, null));
        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle(builder);
        style.bigText(content)
                .setBigContentTitle(titulo)
                .setSummaryText(contenido);

        notificacion.notify(id, builder.build());
    }
}
