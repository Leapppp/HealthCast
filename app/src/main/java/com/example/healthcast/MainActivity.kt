package com.example.healthcast

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.healthcast.fragment.AboutFragment
import com.example.healthcast.fragment.HomeFragment
import com.example.healthcast.fragment.ProfileFragment
import com.example.healthcast.fragment.ScanFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var bottomNavigationView: BottomNavigationView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Setup toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Open navigation drawer when clicking on the toolbar's navigation icon
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val aboutFragment = AboutFragment()
        val homeFragment = HomeFragment()
        val profileFragment = ProfileFragment()
        val scanFragment = ScanFragment()

        replaceFragment(homeFragment)

        // Handle navigation drawer item clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    replaceFragment(homeFragment)
                }

                R.id.nav_profile -> {
                    replaceFragment(profileFragment)
                }

                R.id.nav_about -> {
                    replaceFragment(aboutFragment)
                }

                R.id.nav_calendar -> {
                    // Handle Settings click
                }

                R.id.nav_logout -> {
                    handleLogout()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Handle bottom navigation view item clicks
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bot_home -> {
                    replaceFragment(homeFragment)
                }

                R.id.bot_profile -> {
                    replaceFragment(profileFragment)
                }

                R.id.bot_scan -> {
                    replaceFragment(scanFragment)
                }

                R.id.bot_about -> {
                    replaceFragment(aboutFragment)
                }

                R.id.bot_logout -> {
                    handleLogout()
                }
            }
            true
        }
    }

    fun handleLogout(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout?")
        builder.setPositiveButton("Yes") { dialog, _ ->
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            // Navigate to Login screen or close activity
            val intent = Intent(this, SignInActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.frame_layout, fragment)
            .commit()
    }
}