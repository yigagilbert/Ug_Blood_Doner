package gr.gdschua.bloodapp.Activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.wifi.hotspot2.pps.HomeSp;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

import gr.gdschua.bloodapp.DatabaseAcess.DAOEvents;
import gr.gdschua.bloodapp.DatabaseAcess.DAOHospitals;
import gr.gdschua.bloodapp.Entities.Event;
import gr.gdschua.bloodapp.Entities.Hospital;
import gr.gdschua.bloodapp.R;

public class MapsFragment extends Fragment {
    DAOHospitals daoHospitals = new DAOHospitals();
    DAOEvents daoEvents=new DAOEvents();
    ArrayList<Event> events = new ArrayList<>();
    ArrayList<Hospital> hospitals = new ArrayList<>();

    //get all Events on an array list
    Thread eventThread=new Thread(new Runnable() {
        @Override
        public void run() {
            events = daoEvents.getAllEvents();
        }
    });

    Thread hospThread=new Thread(new Runnable() {
        @Override
        public void run() {
            hospitals = daoHospitals.getAllHospitals();
        }
    });


    private final OnMapReadyCallback callback = new OnMapReadyCallback() {

        @Override
        public void onMapReady(GoogleMap googleMap) {
            eventThread.start();
            hospThread.start();
            FusedLocationProviderClient mFusedLocationClient;

            //if we have permission else just show the events on the map
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                googleMap.setMyLocationEnabled(true);

                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());


                mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location location = task.getResult();
                        if (location == null) {
                            Toast.makeText(getContext(),"CANT GET LOCATIONS",Toast.LENGTH_SHORT).show();
                        } else {
                            LatLng myLoc = new LatLng(location.getLatitude(),location.getLongitude());
                            googleMap.moveCamera(CameraUpdateFactory.newLatLng(myLoc));

                            //Move the camera to the user's location and zoom in!
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 12.0f));
                        }
                    }
                });
                googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(@NonNull Marker marker) {
                        MarkerInfoFragment myMarkerInfoFragment = new MarkerInfoFragment();
                        if(marker.getSnippet().equals("Hospital")){
                            Hospital hospital = (Hospital) marker.getTag();

                            Bundle bundle = new Bundle();
                            bundle.putString("name",hospital.getName());
                            bundle.putString("email",hospital.getEmail());
                            myMarkerInfoFragment.setArguments(bundle);

                        }else if(marker.getSnippet().equals("Event")){
                            Toast.makeText(getContext(),"Event clicked",Toast.LENGTH_LONG).show();
                            return true;
                        }



                        myMarkerInfoFragment.show(getActivity().getSupportFragmentManager(),"My Fragment");
                        return true;
                    }
                });
            }



            //just place the markers :)
            if (events.size()>0) {
                for (int i = 0; i < events.size(); i++) {
                    LatLng eventLatLong = new LatLng(events.get(i).getLat(), events.get(i).getLon());
                    googleMap.addMarker(new MarkerOptions().position(eventLatLong).title(events.get(i).getName()).snippet("Event")).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                }
            }



            //just place the markers :)
            if(hospitals.size()>0) {
                for (int i = 0; i < hospitals.size(); i++) {
                    LatLng hospitalLatLong = new LatLng(hospitals.get(i).getLat(), hospitals.get(i).getLon());
                    googleMap.addMarker(new MarkerOptions().position(hospitalLatLong).title(hospitals.get(i).getName()).snippet("Hospital")).setTag(hospitals.get(i));
                }
            }

        }
    };


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }

        //check if we have permission if not ask nicely if the user denies the app will not close (for now)
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityResultLauncher<String[]> locationPermissionRequest =
                    registerForActivityResult(new ActivityResultContracts
                                    .RequestMultiplePermissions(), result -> {
                                Boolean fineLocationGranted = result.getOrDefault(
                                        Manifest.permission.ACCESS_FINE_LOCATION, false);
                                if (fineLocationGranted != null && fineLocationGranted) {
                                    // Precise location access granted.
                                } else {
                                    Toast.makeText(getContext(),"App will not be able to function properly without location permissions!",Toast.LENGTH_SHORT).show();;
                                }
                            }
                    );

            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            });

            return;
        }


    }
}