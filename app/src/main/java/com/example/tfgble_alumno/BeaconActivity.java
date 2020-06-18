package com.example.tfgble_alumno;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.bluetooth.BluetoothAdapter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.example.tfgble_alumno.MainActivity.getMacAddr;

public class BeaconActivity extends AppCompatActivity {

    private static final String NAME_KEY = "nombre";
    private static final String MAC_KEY = "mac_address";
    protected final String TAG = "Esto es una prueba";

    FirebaseFirestore db;

    Toolbar toolbar;


    private ArrayList<String> assistantList = new ArrayList<>();


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);
        db = FirebaseFirestore.getInstance();
        toolbar = findViewById(R.id.toolbar);

        Bundle bundle = getIntent().getExtras();

        if (bundle != null){
            toolbar.setTitle(bundle.getString("BeaconId"));
        }
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        assistantList.clear();;
        obtenerLista(bundle);

    }

    private void obtenerLista(Bundle bundle) {
        db.collection("Asignaturas").document(bundle.getString("BeaconId")).collection("Clase")
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Alumno alumno = document.toObject(Alumno.class);
                        String nombre = alumno.getNombre();
                        if (!assistantList.contains(nombre)) {
                            assistantList.add(nombre);
                        }
                    }
                } else {
                    Log.w(TAG, "Error getting documents.", task.getException());
                }
            }
        });
    }

    public void saveInfo (View view){

        Random rand = new Random();
        EditText studentNameET = findViewById(R.id.studentNameET);
        final String studentName = studentNameET.getText().toString();
        if (studentName.isEmpty()){
            showToastMessage("Introduce tu nombre completo");
            return;
        }
        String bluetooth = getBluetoothMacAddress();
        Log.e(TAG, bluetooth);
        showToastMessage(bluetooth);
        final String studentMac = getMacAddr();
        Log.e(TAG, studentMac);
        if (studentMac == "00:00:00:00:00:00"){
            showToastMessage("Conecta tu dispositivo a la red inalámbrica");
            return;
        }

        Bundle bundle = getIntent().getExtras();

        DocumentReference doc = db.collection("Asignaturas").document(bundle.getString("BeaconId"));
        doc.collection("Clase")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            String docId = "";
                            String name = "";
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Alumno alumno = document.toObject(Alumno.class);
                                if (alumno.getMac_address().equals(studentMac)) {
                                    docId = document.getId();

                                }
                                if (alumno.getNombre().equals(studentName)){
                                    name = alumno.getNombre();
                                }
                            }
                            if (docId.isEmpty() && name.isEmpty()) {
                                addStudent(studentName, studentMac);
                                assistantList.add(studentName);
                            } else if (assistantList.contains(name)){
                                showToastMessage("Ya existe un registro con ese nombre");
                            } else {
                                update(studentName, docId);
                            }
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    public String getBluetoothMacAddress() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String bluetoothMacAddress = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M){
            try {
                Field mServiceField = bluetoothAdapter.getClass().getDeclaredField("mService");
                mServiceField.setAccessible(true);

                Object btManagerService = mServiceField.get(bluetoothAdapter);

                if (btManagerService != null) {
                    bluetoothMacAddress = (String) btManagerService.getClass().getMethod("getAddress").invoke(btManagerService);
                }
            } catch (NoSuchFieldException e) {

            } catch (NoSuchMethodException e) {

            } catch (IllegalAccessException e) {

            } catch (InvocationTargetException e) {

            }
        } else {
            bluetoothMacAddress = bluetoothAdapter.getAddress();
        }
        return bluetoothMacAddress;
    }

    private void update(final String studentName, String docId) {

        Bundle bundle = getIntent().getExtras();
        DocumentReference doc = db.collection("Asignaturas").document(bundle.getString("BeaconId"));
        doc.collection("Clase").document(docId).update(NAME_KEY, studentName).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "DocumentSnapshot successfully updated!");
                showToastMessage("Registro actualizado");
            }
        });
    }


    private void addStudent(String studentName, String studentMac) {
        if (studentName.isEmpty() || studentMac.isEmpty()){return;}
        Map<String, Object> dataToSave = new HashMap<String, Object>();
        dataToSave.put(NAME_KEY, studentName);
        dataToSave.put(MAC_KEY, studentMac);
        Bundle bundle = getIntent().getExtras();


        db.collection("Asignaturas").document(bundle.getString("BeaconId")).collection("Clase").add(dataToSave)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                        showToastMessage("Registrado con éxito");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });
    }

    private void showToastMessage (String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}
