/*
 * Copyright (C) 2024 Rob Deas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.  

 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.  
 */
@file:Suppress("ktlint:standard:no-wildcard-imports")

package tech.robd.robokey

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import tech.robd.robokey.commands.CommandProcessorService
import tech.robd.robokey.events.*
import tech.robd.robokey.keyboards.LocalRobotKeyboardService.Companion.keyMap
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import kotlin.concurrent.thread

@Component
class SwingMainWindow(
    val appConfig: AppConfig,
    private val commandProcessorService: CommandProcessorService,
    private val systemExitHandler: SystemExitHandler,
    private val coroutineScope: CoroutineScope,
    private val eventGroupProvider: EventsProvider,
) {
    companion object : Logable {
        private val log = setupLogs
    }

    private var isPaused = false // Track if the system is paused
    private lateinit var cardLayout: CardLayout
    private lateinit var contentPanel: JPanel

    // Method to create the help panel
    private fun createHelpPanel(): JPanel {
        val helpPanel = JPanel()
        helpPanel.layout = BorderLayout()

        // Help text area
        val helpText =
            JTextArea(
                """
            RoboKey Help:
            - To send a key, select the key from the dropdown and click 'Send Key'.
            - To send text, type in the text area and click 'Send Text'.
            - Use the Pause/Resume/Stop buttons to control the keyboard commands.
            - The Reset button is available for physical or hardware modes.
            """,
            )
        helpText.isEditable = false
        helpText.lineWrap = true
        helpText.wrapStyleWord = true

        val scrollPane = JScrollPane(helpText)
        helpPanel.add(scrollPane, BorderLayout.CENTER)

        // Exit Button
        val exitButton = JButton("Exit Help")
        exitButton.addActionListener {
            // Return to the main panel when the exit button is clicked
            cardLayout.show(contentPanel, "MainPanel")
        }

        val buttonPanel = JPanel()
        buttonPanel.add(exitButton)

        helpPanel.add(buttonPanel, BorderLayout.SOUTH)

        return helpPanel
    }

    // Method to create the help panel
    private fun createSettingsPanel(): JPanel {
        val helpPanel = JPanel()
        helpPanel.layout = BorderLayout()

        // Help text area
        val helpText =
            JTextArea(
                """
            RoboKey Settings:
            nothing is currently configurable here
            """,
            )
        helpText.isEditable = false
        helpText.lineWrap = true
        helpText.wrapStyleWord = true

        val scrollPane = JScrollPane(helpText)
        helpPanel.add(scrollPane, BorderLayout.CENTER)

        // Exit Button
        val exitButton = JButton("Exit Settings")
        exitButton.addActionListener {
            // Return to the main panel when the exit button is clicked
            cardLayout.show(contentPanel, "MainPanel")
        }

        val buttonPanel = JPanel()
        buttonPanel.add(exitButton)

        helpPanel.add(buttonPanel, BorderLayout.SOUTH)

        return helpPanel
    }

    fun createAndShowGUI() {
        SwingUtilities.invokeLater {
            // Set the look and feel based on darkMode config
            if (appConfig.darkMode) FlatDarkLaf.setup() else FlatLightLaf.setup()

            val frame = JFrame("RoboKey - Keystrokes Configuration")
            frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            frame.layout = BorderLayout()

            // Create Menu Bar
            val menuBar = JMenuBar()
            val fileMenu = JMenu("File")
            val helpMenu = JMenu("Help")

            val settingsMenuItem = JMenuItem("Settings")
            settingsMenuItem.addActionListener {
                // Show the help panel when the Help menu is clicked
                cardLayout.show(contentPanel, "SettingsPanel")
            }

            val exitMenuItem = JMenuItem("Exit")
            exitMenuItem.addActionListener {
                // Exit the application
                val parentEvent =
                    eventGroupProvider.createRootParentEventContext(
                        eventSourceActor = EventSourceActor.GUI,
                        eventCommand = EventCommand.STOP_TYPING,
                    )
                commandProcessorService.stopTyping(parentEvent) // Stop typing before exiting
                systemExitHandler.exitProcess(0)
            }

            // Help Menu Item
            val helpMenuItem = JMenuItem("Help")
            helpMenuItem.addActionListener {
                // Show the help panel when the Help menu is clicked
                cardLayout.show(contentPanel, "HelpPanel")
            }

            fileMenu.add(settingsMenuItem)
            fileMenu.add(exitMenuItem)
            helpMenu.add(helpMenuItem)

            menuBar.add(fileMenu)
            menuBar.add(helpMenu)

            frame.jMenuBar = menuBar

            frame.addWindowListener(
                object : WindowAdapter() {
                    override fun windowClosing(e: WindowEvent?) {
                        log.info("Closing application...")
                        val parentEvent =
                            eventGroupProvider.createRootParentEventContext(
                                eventSourceActor = EventSourceActor.GUI,
                                eventCommand = EventCommand.STOP_TYPING,
                            )
                        // Clean up resources, stop background threads ...
                        commandProcessorService.stopTyping(parentEvent) // Stop typing before exiting
                        coroutineScope.cancel()
                        systemExitHandler.exitProcess(0)
                    }
                },
            )

            // Card layout for switching between main panel and help panel
            cardLayout = CardLayout()
            contentPanel = JPanel(cardLayout)

            // Main Panel
            val mainPanel = createMainPanel(frame)
            contentPanel.add(mainPanel, "MainPanel")

            // Help Panel
            val helpPanel = createHelpPanel()
            contentPanel.add(helpPanel, "HelpPanel")

            // Settings Panel
            val settingsPanel = createSettingsPanel()
            contentPanel.add(settingsPanel, "SettingsPanel")

            // Add content panel to frame
            frame.add(contentPanel, BorderLayout.CENTER)

            frame.setSize(800, 600)
            if (appConfig.gui) frame.isVisible = true
        }
    }

    // Method to create the main panel
    private fun createMainPanel(frame: JFrame): JPanel {
        // Create a label for the current status (e.g., Paused/Resumed)
        val statusLabel = JLabel("Status: Started") // Initial status set to "Started"

        // Create Text Area with Scrollbars
        val textArea = JTextArea()
        textArea.lineWrap = true
        textArea.wrapStyleWord = true
        val scrollPane = JScrollPane(textArea)
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS

        val mainPanel = JPanel(BorderLayout())
        mainPanel.add(scrollPane, BorderLayout.CENTER)
        // First Line of Buttons
        val panel1 = JPanel()
        panel1.layout = FlowLayout(FlowLayout.CENTER, 5, 5) // 5px gaps

        val dropdown = JComboBox<String>()
        dropdown.addItem("")
        keyMap.keys.forEach { dropdown.addItem(it as String?) }

        val sendKeyButton = JButton("Send Key")
        sendKeyButton.isEnabled = false
        val sendTextButton = JButton("Send Text")
        val sendCommandsButton = JButton("Send Commands")
        val clearButton = JButton("Clear")

        // Add buttons to first panel
        panel1.add(clearButton)
        panel1.add(dropdown)
        panel1.add(sendKeyButton)
        panel1.add(sendTextButton)
        panel1.add(sendCommandsButton)

        // Second Line of Buttons
        val panel2 = JPanel()
        panel2.layout = FlowLayout(FlowLayout.CENTER, 5, 5) // 5px gaps

        val stopButton = JButton("Stop")
        val pauseButton = JButton("Pause")
        val resumeButton = JButton("Resume")
        val resetButton = JButton("Reset")
        resetButton.isEnabled = appConfig.mode.uppercase() in listOf("PHYSICAL", "HARDWARE")

        val radioButton = JRadioButton("Keyboard ON")
        radioButton.isSelected = true

        // Add buttons to second panel
        panel2.add(stopButton)
        panel2.add(pauseButton)
        panel2.add(resumeButton)
        panel2.add(resetButton)
        panel2.add(radioButton)

        // Add panels to main panel
        mainPanel.add(panel1)
        mainPanel.add(panel2)

        // Create a panel to hold both the buttons and the status label
        val bottomPanel = JPanel()
        bottomPanel.layout = BoxLayout(bottomPanel, BoxLayout.Y_AXIS)

        // Status Panel with FlowLayout.RIGHT and horizontal gap for padding
        val statusPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 25, 5)) // 25px horizontal padding
        statusPanel.add(statusLabel)

        // Add both button panels to the bottom panel
        bottomPanel.add(panel1)
        bottomPanel.add(panel2)
        bottomPanel.add(statusPanel)

        // Add scroll pane (text area) to mainPanel directly
        mainPanel.add(scrollPane)
        // Add bottom panel to the main panel at BorderLayout.SOUTH
        mainPanel.add(bottomPanel, BorderLayout.SOUTH)

        // Handle Button Actions
        // Handle Key Sending
        sendKeyButton.addActionListener {
            coroutineScope.launch(Dispatchers.IO) {
                val selectedKey = dropdown.selectedItem as? String
                if (selectedKey != null && keyMap.containsKey(selectedKey)) {
                    log.info("Selected key: $selectedKey is contained in the map.")
                    disableButtonTemporarily(sendKeyButton) {
                        val parentEvent =
                            eventGroupProvider.createRootParentEventContext(
                                eventSourceActor = EventSourceActor.GUI,
                                eventCommand = EventCommand.KEY_PRESS,
                                eventValue = textArea.text,
                            )
                        sendKeyCommand(selectedKey, parentEvent)
                    }
                } else {
                    log.info("Selected key: $selectedKey is not contained in the map.")
                }
                SwingUtilities.invokeLater {
                    // Update the UI when done
                    log.debug("Sent key")
                }
            }
        }

        // Add ActionListener to dropdown to enable/disable sendKeyButton
        dropdown.addActionListener {
            val selectedKey = dropdown.selectedItem as? String
            sendKeyButton.isEnabled = !selectedKey.isNullOrEmpty()
        }

        // Handle Text Sending
        sendTextButton.addActionListener {
            disableButtonTemporarily(sendTextButton) {
                val parentEvent =
                    eventGroupProvider.createRootParentEventContext(
                        eventSourceActor = EventSourceActor.GUI,
                        eventCommand = EventCommand.TEXT,
                        eventValue = textArea.text,
                    )
                sendTextCommand(textArea.text, parentEvent)
            }
        }

        // Handle Command Sending
        sendCommandsButton.addActionListener {
            disableButtonTemporarily(sendCommandsButton) {
                val parentEvent =
                    eventGroupProvider.createEventBatch(
                        eventSourceActor = EventSourceActor.GUI,
                        batchContents = textArea.text, // Pass as batchContents
                    )
                sendCommands(textArea.text.lines(), parentEvent)
            }
        }

        // Clear text area
        clearButton.addActionListener {
            textArea.text = ""
            dropdown.selectedIndex = -1
            radioButton.isSelected = true // select radio button on clear fakeKeyboardService back on
        }

        // Stop typing
        stopButton.addActionListener {
            coroutineScope.launch(Dispatchers.IO) {
                val parentEvent =
                    eventGroupProvider.createRootParentEventContext(
                        eventSourceActor = EventSourceActor.GUI,
                        eventCommand = EventCommand.STOP_TYPING,
                    )
                commandProcessorService.stopTyping(parentEvent)
                statusLabel.text = "Status: Stopped"
                isPaused = false

                // Disable pause and but enable resume button when stopped
                pauseButton.isEnabled = false
                resumeButton.isEnabled = true
                SwingUtilities.invokeLater {
                    // Update the UI when done
                    log.debug("Task started!")
                }
            }
        }

        pauseButton.addActionListener {
            coroutineScope.launch(Dispatchers.IO) {
                isPaused = true
                statusLabel.text = "Status: Paused"
                val parentEvent =
                    eventGroupProvider.createRootParentEventContext(
                        eventSourceActor = EventSourceActor.WEB,
                        eventCommand = EventCommand.PAUSE_TYPING,
                    )
                commandProcessorService.pauseTyping(parentEvent)

                // Disable pause and enable resume buttons
                pauseButton.isEnabled = false
                resumeButton.isEnabled = true
                stopButton.isEnabled = true
                SwingUtilities.invokeLater {
                    // Update the UI when done
                    log.debug("Task started!")
                }
            }
        }

        resumeButton.addActionListener {
            coroutineScope.launch(Dispatchers.IO) {
                isPaused = false
                statusLabel.text = "Status: Resumed"
                val parentEvent =
                    eventGroupProvider.createRootParentEventContext(
                        eventSourceActor = EventSourceActor.WEB,
                        eventCommand = EventCommand.RESUME_TYPING,
                    )
                commandProcessorService.resumeTyping(parentEvent)

                // Enable pause and stop but disable resume buttons
                pauseButton.isEnabled = true
                stopButton.isEnabled = true
                resumeButton.isEnabled = false
                SwingUtilities.invokeLater {
                    // Update the UI when done
                    log.debug("Task started!")
                }
            }
        }

        // Reset action
        resetButton.addActionListener {
            coroutineScope.launch(Dispatchers.IO) {
                if (commandProcessorService.getCommandProcessor().isUsingArduino() ||
                    commandProcessorService.getCommandProcessor().isUsingLocalRobot()
                ) {
                    val parentEvent =
                        eventGroupProvider.createRootParentEventContext(
                            eventSourceActor = EventSourceActor.WEB,
                            eventCommand = EventCommand.RESET_KEYBOARD,
                        )
                    commandProcessorService.resetKeyboardProcessor(parentEvent)
                }
                pauseButton.isEnabled = true
                SwingUtilities.invokeLater {
                    // Update the UI when done
                    log.debug("Task started!")
                }
            }
        }

        // Handle Radio Button Changes
        radioButton.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                // Radio button was selected
                sendTextButton.isEnabled = true
                resumeButton.isEnabled = false
                stopButton.isEnabled = true
                pauseButton.isEnabled = true
                val selectedKey = dropdown.selectedItem as? String
                sendKeyButton.isEnabled = !selectedKey.isNullOrEmpty()
                sendResetAction()
            } else if (e.stateChange == ItemEvent.DESELECTED) {
                // Radio button was deselected
                pauseButton.isEnabled = false
                sendTextButton.isEnabled = false
                sendKeyButton.isEnabled = false
                resumeButton.isEnabled = true
                stopButton.isEnabled = false
                sendStopAction()
            }
        }

        return mainPanel
    }

    // Send Key Command via CommandProcessor
    private fun sendKeyCommand(
        selectedKey: String,
        parentEvent: CommandEventContext,
    ) {
        val cmdTextLines = "key:$selectedKey"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                commandProcessorService.getCommandProcessor().enqueueCommand(cmdTextLines, parentEvent) // Use CommandProcessor
            } catch (e: Exception) {
                log.info("Error sending key command: ${e.message}")
            }
        }
    }

    private fun sendTextCommand(
        text: String,
        parentEvent: CommandEventContext,
    ) {
        val lines = text.split(Regex("\r?\n"))
        val endsWithNewline = text.endsWith("\n")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val commandProcessor = commandProcessorService.getCommandProcessor()

                for ((index, line) in lines.withIndex()) {
                    val isLastLine = index == lines.lastIndex
                    val commandPrefix = if (isLastLine && !endsWithNewline) "text:" else "line:"
                    val cmdTextLine = "$commandPrefix$line"
                    commandProcessor.enqueueCommand(cmdTextLine, parentEvent)
                }
            } catch (e: Exception) {
                log.info("Error sending text command: ${e.message}")
            }
        }
    }

    // Send Multiple Commands via CommandProcessor
    private fun sendCommands(
        lines: List<String>,
        parentEvent: EventGroup,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                lines.forEach { command ->
                    commandProcessorService.getCommandProcessor().enqueueCommand(command, parentEvent) // Queue each command
                }
            } catch (e: Exception) {
                log.info("Error sending commands: ${e.message}")
            }
        }
    }

    // Disables a button temporarily to prevent multiple clicks during execution
    private fun disableButtonTemporarily(
        button: JButton,
        action: () -> Unit,
    ) {
        button.isEnabled = false
        thread {
            if (shouldDelay()) {
                Thread.sleep(1500) // Sleep on a background thread, not the UI thread
            }
            SwingUtilities.invokeLater {
                action()
                Timer(500) { button.isEnabled = true }.start() // Re-enable after 500ms
            }
        }
    }

    // Determine whether to delay before sending commands
    private fun shouldDelay(): Boolean = appConfig.mode.uppercase() in listOf("LOCAL", "VIRTUAL") || appConfig.keyboard.uiExtraDelay

    // Actions for Radio Button State Changes
    private fun sendStopAction() {
        log.info("Radio button selected, sending stop action")
        val parentEvent =
            eventGroupProvider.createRootParentEventContext(
                eventSourceActor = EventSourceActor.WEB,
                eventCommand = EventCommand.STOP_TYPING,
            )
        commandProcessorService.stopTyping(parentEvent)
    }

    private fun sendResetAction() {
        log.info("Radio button deselected, sending reset action")
        val parentEvent =
            eventGroupProvider.createRootParentEventContext(
                eventSourceActor = EventSourceActor.WEB,
                eventCommand = EventCommand.RESET_KEYBOARD,
            )
        commandProcessorService.resumeTyping(parentEvent)
    }
}
