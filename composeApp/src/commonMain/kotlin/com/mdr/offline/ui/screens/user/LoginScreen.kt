package com.mdr.offline.ui.screens.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mdr.offline.ui.MangaDexTheme
import com.mdr.offline.ui.navigation.user.AuthenticationComponent
import mangadexoffline.composeapp.generated.resources.Res
import mangadexoffline.composeapp.generated.resources.ic_hide
import mangadexoffline.composeapp.generated.resources.ic_show
import org.jetbrains.compose.resources.painterResource

@Composable
fun LoginScreenContent(
    component: AuthenticationComponent
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,

        modifier = Modifier
            .fillMaxSize()
            .background(MangaDexTheme.color.background)
    ) {
        LoginForm(component)
    }
}

// TODO Make it look better on phone

@Composable
private fun LoginForm(
    component: AuthenticationComponent
) {
    var login by remember { mutableStateOf<String>("") }
    var password by remember { mutableStateOf<String>("") }

    var passwordVisible by remember { mutableStateOf<Boolean>(false) }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(MangaDexTheme.color.primary)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(48.dp)
        ) {
            OutlinedTextField(
                value = login,
                onValueChange = { login = it },
                placeholder = { Text("Login") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MangaDexTheme.color.mainButton,
                    unfocusedBorderColor = MangaDexTheme.color.white,
                    focusedPlaceholderColor = MangaDexTheme.color.white,
                    cursorColor = MangaDexTheme.color.mainButton,
                    focusedTextColor = MangaDexTheme.color.white,
                    unfocusedTextColor = MangaDexTheme.color.white,
    //                focusedIndicatorColor = MangaDexTheme.color.white,
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible)
                        painterResource(Res.drawable.ic_hide)
                    else painterResource(Res.drawable.ic_show)

                    // Please provide localized description for accessibility services
                    val description = if (passwordVisible) "Hide password" else "Show password"

                    IconButton(onClick = {passwordVisible = !passwordVisible}){
                        Icon(painter = image, description)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MangaDexTheme.color.mainButton,
                    unfocusedBorderColor = MangaDexTheme.color.white,
                    focusedPlaceholderColor = MangaDexTheme.color.white,
                    cursorColor = MangaDexTheme.color.mainButton,
                    focusedTextColor = MangaDexTheme.color.white,
                    unfocusedTextColor = MangaDexTheme.color.white,
                    focusedTrailingIconColor = MangaDexTheme.color.white,
                    unfocusedTrailingIconColor = MangaDexTheme.color.white,
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = {component.logIn(login, password)},
                colors = ButtonDefaults.buttonColors(
                    containerColor = MangaDexTheme.color.mainButton,
                    contentColor = MangaDexTheme.color.white
                ),
                modifier = Modifier
                    .padding(bottom = 16.dp)
            ) {
                Text("Log in")
            }

            Text(component.errorMessage.value, color = Color.Red)
        }
    }
}