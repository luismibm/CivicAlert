package com.luismibm.civicalert.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.luismibm.civicalert.databinding.FragmentHomeBinding
import java.io.IOException
import java.util.Locale
import java.util.concurrent.Executors

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var locationPermissionRequest: ActivityResultLauncher<Array<String>>
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private var mTrackingLocation = false
    private lateinit var mLocationCallback: LocationCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
            val coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
            if (fineLocationGranted) {
                startTrackingLocation()
            } else if (coarseLocationGranted) {
                startTrackingLocation()
            } else {
                Toast.makeText(requireContext(), "No Permissions Granted", Toast.LENGTH_SHORT).show()
            }
        }

        mLocationCallback = object : LocationCallback() {
            @SuppressLint("SetTextI18n")
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult.lastLocation != null) {
                    fetchAddress(locationResult.lastLocation!!)
                } else {
                    binding.textHome.setText("No location known")
                }
            }
        }
        binding.buttonLocation.setOnClickListener {
            Toast.makeText(requireContext(), "Clicked: GET LOCATION", Toast.LENGTH_SHORT).show()
            if (!mTrackingLocation) {
                startTrackingLocation()
            } else {
                stopTrackingLocation()
            }
        }
        return root

    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun startTrackingLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Request Permissions", Toast.LENGTH_SHORT).show()
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            Toast.makeText(requireContext(), "startTrackingLocation() permissions granted", Toast.LENGTH_SHORT).show()
            mFusedLocationClient.requestLocationUpdates(getLocationRequest(), mLocationCallback, null)
        }
        binding.textHome.setText("Loading...")
        binding.progressBar.setVisibility(ProgressBar.VISIBLE)
        mTrackingLocation = true
        binding.buttonLocation.setText("Stop Tracking Location")
    }

    private fun getLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()
        return locationRequest
    }

    @SuppressLint("SetTextI18n")
    private fun stopTrackingLocation() {
        if (mTrackingLocation) {
            binding.apply {
                progressBar.visibility = ProgressBar.INVISIBLE
                textHome.setText("Start Tracking Location")
            }
            mTrackingLocation = false
            binding.buttonLocation.setText("Start Tracking Location")
            mFusedLocationClient.removeLocationUpdates(mLocationCallback)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun fetchAddress(location: Location) {
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        executor.execute {
            var addresses: List<Address>? = null
            var resultMessage = ""
            try {
                addresses = geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                )
                if (addresses == null || addresses.size == 0) {
                    if (resultMessage.isEmpty()) {
                        resultMessage = "No Address Found"
                    }
                } else {
                    val address= addresses[0]
                    val addressParts: ArrayList<String> = ArrayList()
                    for (i in 0..address.maxAddressLineIndex) {
                        addressParts.add(address.getAddressLine(i))
                    }
                    resultMessage = TextUtils.join("\n", addressParts)
                    var finalResultMessage = resultMessage
                    handler.post {
                        if (mTrackingLocation) {
                            val time = java.text.DateFormat.getTimeInstance().format(System.currentTimeMillis())
                            binding.textHome.text = "Location: $finalResultMessage \n Time: $time"
                        }
                    }
                }
            } catch (e: IOException) {
                resultMessage = "fetchAddress() IOException"
            } catch (e: IllegalArgumentException) {
                resultMessage = "fetchAddress() illegalArgumentException"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}