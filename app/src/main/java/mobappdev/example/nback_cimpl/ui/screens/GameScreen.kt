package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.animation.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.R
import mobappdev.example.nback_cimpl.ui.theme.Purple40
import mobappdev.example.nback_cimpl.ui.theme.PurpleGrey40
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameState
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel

@Composable
fun GameScreen(
    vm: GameViewModel,
    onBackToHome: () -> Unit) {

    val gameState by vm.gameState.collectAsState()
    val score by vm.score.collectAsState()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LaunchedEffect( Unit) {
            vm.startGame()
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "N-Back Game", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(20.dp))

        // Display current score and high score
        Text(text = "Score: $score", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Event number: ${gameState.eventValue}", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(20.dp))

        if (gameState.roundFinished){

            val persentage = (gameState.urMatches.toFloat()/gameState.actualMatches)*100

           Text("Round finished with point score of $score out of ${gameState.actualMatches*gameState.nBackValue}\n" +
                   "Matches: ${gameState.urMatches} out of ${gameState.actualMatches}\n" +
                   "With a $persentage%")

            Button(onClick = {
                vm.resetGame()
                onBackToHome() }) {
                Text("Back Home")
            }
        }else {
            if (gameState.gameType == GameType.Visual) GridVisualGame(gameState)
            if (gameState.gameType == GameType.Audio) {
                Icon(
                painter = painterResource(id = R.drawable.sound_on),
                contentDescription = "Sound",
                modifier = Modifier
                    .height(48.dp)
                    .aspectRatio(3f / 2f)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            MatchButton(vm)


        }
        Spacer(modifier = Modifier.height(60.dp))
        
    }
}

@Composable
fun MatchButton(vm: GameViewModel){
    //var isMatchFound by remember { mutableStateOf(true) }
    var buttonColor by remember { mutableStateOf(Purple40) } // Initial color
    val coroutineScope = rememberCoroutineScope() // Coroutine scope for managing coroutines

    Button(onClick = {
        val isMatchFound =  vm.checkMatch()

        buttonColor = if (isMatchFound) Color.Green else Color.Red

        coroutineScope.launch {
            delay(400) // Delay for 500ms
            buttonColor = Purple40 // Revert to original color
        }
    },
        modifier = Modifier.height(100.dp)
            .width(240.dp),
        colors = ButtonDefaults.buttonColors(buttonColor)
    ) {
        Text(
            "Match",
            style = MaterialTheme.typography.displayLarge
        )
    }

}



@Composable
fun GridVisualGame(gameState: GameState){
    // 3x3 Grid for visual cues
    Box(
        modifier = Modifier
            .size(300.dp)
            .padding(16.dp)
    ) {
        Column {
            for (row in 0 until 3) {
                Row(
                    modifier = Modifier.weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (col in 0 until 3) {
                        val position = (row * 3 + col)+1
                        val animatableColor = remember { Animatable(Color.LightGray) }

                        // Trigger the blink effect whenever the position matches `gameState.eventValue`
                        LaunchedEffect(gameState.eventIndex ) {
                            if (position == gameState.eventValue ) {
                                // Animate to green
                                animatableColor.animateTo(
                                    targetValue = Color.Green,
                                    animationSpec = tween(durationMillis = 500) // Blink duration
                                )
                                delay(500)
                                animatableColor.animateTo(
                                    targetValue = Color.LightGray,
                                    animationSpec = tween(durationMillis = 500) // Instant revert to gray
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    animatableColor.value,
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun GameScreenPreview() {
    // Since I am injecting a VM into my homescreen that depends on Application context, the preview doesn't work.
    Surface(){
        GameScreen(
            FakeVM(),
            onBackToHome = { }
        )
    }
}
