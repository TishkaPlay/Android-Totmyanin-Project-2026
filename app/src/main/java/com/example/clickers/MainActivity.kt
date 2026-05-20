// Пакет приложения (должен совпадать с вашим)
package com.example.clickers

// Импорты для работы Compose и Android
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel

/*
    ViewModel – хранит данные игры
    Живёт дольше, чем экран (например, при повороте телефона данные не теряются).
*/
class MainViewModel : ViewModel() {
    //mutableIntStateOf – специальная переменная для чисел, которая обновляет UI при изменении
    private var _points by mutableIntStateOf(0)      //очки игрока
    val points: Int get() = _points                  //публичное неизменяемое поле

    private var _clickPower by mutableIntStateOf(1)  // колько очков даёт один клик
    val clickPower: Int get() = _clickPower

    private var _upgradeLevel by mutableIntStateOf(0) //уровень улучшения (сколько раз купили)
    val upgradeLevel: Int get() = _upgradeLevel

    private var _totalClicks by mutableIntStateOf(0)  //статистика: всего нажатий за всё время
    val totalClicks: Int get() = _totalClicks

/*
    Стоимость следующего улучшения.
    Формула: (уровень + 1) * 10 → 10, 20, 30, ...
*/
    val upgradeCost: Int
        get() = (_upgradeLevel + 1) * 10

/*
    Действие при клике по главной кнопке.
    Добавляет очки в соответствии с силой клика и увеличивает счётчик кликов.
*/
    fun click() {
        _points += clickPower
        _totalClicks++
    }

/*
    Покупка улучшения.
    Если хватает очков – списываем цену, повышаем уровень и силу клика.
*/
    fun buyUpgrade() {
        if (points >= upgradeCost) {
            _points -= upgradeCost
            _upgradeLevel++
            _clickPower++   //сила клика увеличивается на 1
        }
    }

    //Полный сброс игры: обнуляем все значения.
    fun resetGame() {
        _points = 0
        _clickPower = 1
        _upgradeLevel = 0
        _totalClicks = 0
    }
}

/*
    Закрытое перечисление (sealed class) для навигации между экранами.
    У нас есть три возможных экрана: главный, статистика, улучшения.
*/
sealed class Screen {
    object Main : Screen()
    object Stats : Screen()
    object Upgrades : Screen()
}

/*
    Главная Activity – точка входа в приложение.
    Устанавливает Compose-интерфейс с помощью setContent.
*/
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            //MaterialTheme – стандартная тема Compose
            MaterialTheme {
                ClickerApp()   //вызываем основной композабл приложения
            }
        }
    }
}

//Основной композабл, который управляет навигацией и диалогом сброса.
@Composable
fun ClickerApp() {
    //Запоминаем, какой экран сейчас открыт (по умолчанию главный)
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
    //Флаг для показа диалога подтверждения сброса
    var showResetDialog by remember { mutableStateOf(false) }
    //Получаем экземпляр ViewModel (автоматически создаётся и сохраняется)
    val viewModel: MainViewModel = viewModel()

    //Если флаг true – показываем диалог поверх всего
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Сброс игры") },
            text = { Text("Все очки и улучшения будут потеряны. Продолжить?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetGame()   // сброс данных
                        showResetDialog = false
                    }
                ) { Text("Да") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Нет") }
            }
        )
    }

    //Красивый вертикальный градиент для фона (темно-синие оттенки)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E))
    )

    //Контейнер Box, который занимает весь экран и имеет фон-градиент
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        //В зависимости от текущего экрана показываем нужный композабл
        when (currentScreen) {
            Screen.Main -> MainScreen(
                points = viewModel.points,
                clickPower = viewModel.clickPower,
                onMainClick = { viewModel.click() },
                onStatsClick = { currentScreen = Screen.Stats },
                onUpgradesClick = { currentScreen = Screen.Upgrades },
                onResetClick = { showResetDialog = true }
            )
            Screen.Stats -> StatsScreen(
                totalClicks = viewModel.totalClicks,
                clickPower = viewModel.clickPower,
                upgradeLevel = viewModel.upgradeLevel,
                points = viewModel.points,
                onBackClick = { currentScreen = Screen.Main }
            )
            Screen.Upgrades -> UpgradesScreen(
                upgradeLevel = viewModel.upgradeLevel,
                clickPower = viewModel.clickPower,
                upgradeCost = viewModel.upgradeCost,
                points = viewModel.points,
                onBuyUpgrade = { viewModel.buyUpgrade() },
                onBackClick = { currentScreen = Screen.Main }
            )
        }
    }
}

/*
    Главный экран кликера.
    Здесь отображается счёт, сила клика, большая кнопка "КЛИК" и три кнопки действий.
*/
@Composable
fun MainScreen(
    points: Int,
    clickPower: Int,
    onMainClick: () -> Unit,
    onStatsClick: () -> Unit,
    onUpgradesClick: () -> Unit,
    onResetClick: () -> Unit
) {
    //Вертикальное расположение элементов (Column) с выравниванием по центру
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center   //элементы равномерно по центру по вертикали
    ) {
        //Счёт (крупный текст)
        Text(
            text = "$points",
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 2.sp
        )
        Text(
            text = "очков",
            fontSize = 24.sp,
            color = Color.LightGray,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        //Сила клика с эмодзи
        Text(
            text = "⚡ +$clickPower за клик",
            fontSize = 18.sp,
            color = Color(0xFFFFD966)
        )
        Spacer(modifier = Modifier.height(48.dp))

        //Большая круглая кнопка "КЛИК" с тенью
        Button(
            onClick = onMainClick,
            modifier = Modifier
                .size(200.dp)
                .shadow(20.dp, shape = RoundedCornerShape(100.dp)), //тень для объёма
            shape = RoundedCornerShape(100.dp),   //полностью круглая
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF5722),  //оранжевый
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
        ) {
            Text(
                text = "КЛИК",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(56.dp))

        //Ещё одна колонка для трёх кнопок действий (вертикально, с отступами)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)  //расстояние между кнопками 16dp
        ) {
            ActionButton("📊 Статистика", onStatsClick, Color(0xFF3F51B5))
            ActionButton("⬆ Улучшения", onUpgradesClick, Color(0xFF009688))
            ActionButton("🔄 Сбросить игру", onResetClick, Color(0xFF795548))
        }
    }
}

/*
    Переиспользуемый компонент для кнопок действий (статистика, улучшения, сброс).
    Имеет фиксированную ширину (70% экрана), высоту 56dp, скруглённые углы и тень.
*/
@Composable
fun ActionButton(text: String, onClick: () -> Unit, color: Color) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.7f)   //занимает 70% ширины родительской колонки
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),   //сильно скруглённые углы (капсула)
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/*
    Экран статистики.
    Показывает общее количество кликов, очки, силу клика и уровень улучшения.
*/
@Composable
fun StatsScreen(
    totalClicks: Int,
    clickPower: Int,
    upgradeLevel: Int,
    points: Int,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📈 СТАТИСТИКА", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(32.dp))

        // Четыре строки с данными (сделаны через отдельную функцию StatItem)
        StatItem("Всего кликов", totalClicks)
        StatItem("Текущие очки", points)
        StatItem("Сила клика", clickPower)
        StatItem("Уровень улучшения", upgradeLevel)

        Spacer(Modifier.height(48.dp))
        Button(onClick = onBackClick) { Text("◀ На главную") }
    }
}

/*
    Одна строка для статистики: название и значение.
    Используется Row для размещения текста слева и числа справа.
*/

@Composable
fun StatItem(title: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween   // текст слева, число справа
    ) {
        Text(title, fontSize = 20.sp, color = Color.White)
        Text(value.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Yellow)
    }
    Spacer(Modifier.height(12.dp))   // отступ после строки
}

/*
    Экран магазина улучшений.
    Позволяет купить +1 к силе клика за очки.
*/
@Composable
fun UpgradesScreen(
    upgradeLevel: Int,
    clickPower: Int,
    upgradeCost: Int,
    points: Int,
    onBuyUpgrade: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🛒 МАГАЗИН УЛУЧШЕНИЙ", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(32.dp))

        //Карточка с информацией об улучшении
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C3E50))  // тёмно-синий фон карточки
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Усиление клика", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Уровень: $upgradeLevel → +$clickPower за клик", color = Color.LightGray)
                Text("Следующий: +${upgradeLevel + 1} за клик", color = Color.LightGray)
                Spacer(Modifier.height(12.dp))
                Text("Стоимость: $upgradeCost очков", fontSize = 20.sp, color = Color.Yellow)
                Spacer(Modifier.height(16.dp))

                //Кнопка покупки. enabled = points >= upgradeCost – блокируется, если не хватает очков
                Button(
                    onClick = onBuyUpgrade,
                    enabled = points >= upgradeCost,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Купить")
                }

                //Показываем сообщение, если очков недостаточно
                if (points < upgradeCost) {
                    Text(
                        text = "Не хватает ${upgradeCost - points} очков",
                        color = Color.Red,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(48.dp))
        Button(onClick = onBackClick) { Text("◀ На главную") }
    }
}