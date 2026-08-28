package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.components.CollideShell
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        CollideShell()
      }
    }
  }
}

@Preview(showBackground = true, widthDp = 1440, heightDp = 900)
@Composable
fun CollideShellWidePreview() {
  MyApplicationTheme {
    CollideShell()
  }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
fun CollideShellMobilePreview() {
  MyApplicationTheme {
    CollideShell()
  }
}

