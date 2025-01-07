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
 *
 * ----------------------------------------------------------------------
 * This program emulates A VIRTUAL KEYBOARD using an Arduino Due
 * it allows commands to be sent such as lorem which will cause the Arduino to type Lorem Ipsum.
 */
#include <Keyboard.h>
#include <sam.h>
#include <HID.h>
#include <stdint.h>
#include <Arduino.h>
#include <base64.hpp>
#include <base64.hpp>

// Define constants and structs
struct KeyMapping {
  const char* text;
  uint8_t keyCode;
};


enum CommandType {
  HELP,
  TYPE_HELP,
  PING,
  STATUS,
  LOREM,
  LOREM_LINES,
  TEXT,
  LINE,
  KEY,
  COMBO,
  EDIT,
  HOLD,
  CMD_OUTPUT_OFF,
  CMD_OUTPUT_ON,
  CMD_SET_DELAY,
  CMD_SET_PRESS_LENGTH,
  CMD_RESET,
  CMD_ECHO_ON,
  CMD_ECHO_OFF,
  CMD_DEBUG_ON,
  CMD_DEBUG_OFF,
  CMD_JITTER_ON,
  CMD_JITTER_OFF,
  CMD_KEY_JITTER_ON,
  CMD_KEY_JITTER_OFF,
  CMD_DELAY_JITTER_ON,
  CMD_DELAY_JITTER_OFF,
  CMD_SET_KEY_JITTER_VALUE,
  CMD_SET_DELAY_JITTER_VALUE,
  CMD_CONNECT,
  CMD_DISCONNECT,
  CMD_RECONNECT,
  CMD_SIMULATE_ON,
  CMD_SIMULATE_OFF,
  CMD_STOP,
  CMD_PAUSE,
  CMD_RESUME,
  RELEASE_KEY,
  RELEASE_ALL_KEYS,
  PASSWORD,
  PRIVATE_TEXT,
  INVALID
};


// Define UuidClearOption before any functions that use it
enum UuidClearOption {
  NO_CLEAR,  // Don't clear any UUID
  PRIORITY,  // Clear only priorityUUID
  NORMAL,    // Clear only normalUUID
  BOTH       // Clear both UUIDs
};


// Define Command structure before usage
struct Command {
  const char* name;
  CommandType type;
  bool hasParameter;
};


String sanitizeSensitiveLog(CommandType type, const String& message, const String& uuid) {
  switch (type) {
    case PASSWORD:
      return "Sensitive information (password) redacted for UUID: " + uuid;
    case PRIVATE_TEXT:
      return "Sensitive information (private text) redacted for UUID: " + uuid;
    default:
      return message;  // No sensitive information, return the original message
  }
}


Command knownCommands[] = {
  { "HELP", HELP, false },
  { "TYPE:HELP", TYPE_HELP, false },
  { "PING", PING, false },
  { "STATUS", STATUS, false },
  { "LOREM", LOREM, false },
  { "LOREM:LINES", LOREM_LINES, false },
  { "TEXT:", TEXT, true },
  { "LINE:", LINE, true },
  { "KEY:", KEY, true },
  { "COMBO:", COMBO, true },
  { "EDIT:", EDIT, true },
  { "HOLD:", HOLD, true },
  { "CMD:SET:DELAY:", CMD_SET_DELAY, true },
  { "CMD:SET:DELAY:JITTER:MAX:", CMD_SET_DELAY_JITTER_VALUE, true },
  { "CMD:SET:KEY:JITTER:MAX:", CMD_SET_KEY_JITTER_VALUE, true },
  { "CMD:SET:PRESS:LENGTH:", CMD_SET_PRESS_LENGTH, true },
  { "RELEASE:KEY:", RELEASE_KEY, true },
  { "RELEASE:ALL", RELEASE_ALL_KEYS, false },
  { "CMD:OUTPUT:OFF", CMD_OUTPUT_OFF, false },
  { "CMD:OUTPUT:ON", CMD_OUTPUT_ON, false },
  { "CMD:RESET", CMD_RESET, false },
  { "CMD:ECHO:ON", CMD_ECHO_ON, false },
  { "CMD:ECHO:OFF", CMD_ECHO_OFF, false },
  { "CMD:DEBUG:ON", CMD_DEBUG_ON, false },
  { "CMD:DEBUG:OFF", CMD_DEBUG_OFF, false },
  { "CMD:JITTER:ON", CMD_JITTER_ON, false },
  { "CMD:JITTER:OFF", CMD_JITTER_OFF, false },
  { "CMD:KEY:JITTER:ON", CMD_KEY_JITTER_ON, false },
  { "CMD:KEY:JITTER:OFF", CMD_KEY_JITTER_OFF, false },
  { "CMD:DELAY:JITTER:ON", CMD_DELAY_JITTER_ON, false },
  { "CMD:DELAY:JITTER:OFF", CMD_DELAY_JITTER_OFF, false },
  { "CMD:CONNECT", CMD_CONNECT, false },
  { "CMD:DISCONNECT", CMD_DISCONNECT, false },
  { "CMD:RECONNECT", CMD_RECONNECT, false },
  { "CMD:STOP", CMD_STOP, false },
  { "CMD:PAUSE", CMD_PAUSE, false },
  { "CMD:RESUME", CMD_RESUME, false },
  { "PASSWORD:", PASSWORD, true },
  { "PRIVATE:TEXT:", PRIVATE_TEXT, true },
};

String base64Decode(const String& encoded) {
  size_t outputLength = 0;                  // Output length isn't directly returned by this library
  unsigned char decoded[encoded.length()];  // Allocate space for decoded data (adjust size if needed)

  // Decode the Base64 string
  unsigned int length = decode_base64(
    (unsigned char*)encoded.c_str(),  // Input: Base64 encoded string
    encoded.length(),                 // Length of the input string
    decoded                           // Output: Decoded binary data
  );

  // Convert decoded output to String and return
  return String((char*)decoded).substring(0, length);
}

String commandTypeToString(CommandType type) {
  // Iterate over knownCommands array to find a matching CommandType
  for (const Command& cmd : knownCommands) {
    if (cmd.type == type) {
      return cmd.name;  // Return the name if a match is found
    }
  }
  return "UNKNOWN";  // Return "UNKNOWN" if the CommandType is not found
}

// Define an array of key mappings
KeyMapping keyMappings[] = {
  { "@C", KEY_LEFT_CTRL },
  { "@S", KEY_LEFT_SHIFT },
  { "@A", KEY_LEFT_ALT },
  { "\\R", KEY_RETURN },
  { "\\N", KEY_RETURN },
  { "\\B", KEY_BACKSPACE },
  { "\\T", KEY_TAB },
  { "UP", KEY_UP_ARROW },
  { "DOWN", KEY_DOWN_ARROW },
  { "LEFT", KEY_LEFT_ARROW },
  { "RIGHT", KEY_RIGHT_ARROW },
  { "CTRL", KEY_LEFT_CTRL },
  { "SHIFT", KEY_LEFT_SHIFT },
  { "ALT", KEY_LEFT_ALT },
  { "GUI", KEY_LEFT_GUI },
  { "ALT_GR", KEY_RIGHT_ALT },
  { "DEL", KEY_DELETE },
  { "BS", KEY_BACKSPACE },
  { "BKSP", KEY_BACKSPACE },
  { "LEFT_CTRL", KEY_LEFT_CTRL },
  { "LEFT_SHIFT", KEY_LEFT_SHIFT },
  { "LEFT_ALT", KEY_LEFT_ALT },
  { "LEFT_GUI", KEY_LEFT_GUI },
  { "RIGHT_CTRL", KEY_RIGHT_CTRL },
  { "RIGHT_SHIFT", KEY_RIGHT_SHIFT },
  { "RIGHT_ALT", KEY_RIGHT_ALT },
  { "RIGHT_GUI", KEY_RIGHT_GUI },
  { "UP_ARROW", KEY_UP_ARROW },
  { "DOWN_ARROW", KEY_DOWN_ARROW },
  { "LEFT_ARROW", KEY_LEFT_ARROW },
  { "RIGHT_ARROW", KEY_RIGHT_ARROW },
  { "BACKSPACE", KEY_BACKSPACE },
  { "TAB", KEY_TAB },
  { "RETURN", KEY_RETURN },
  { "ENTER", KEY_RETURN },
  { "MENU", KEY_MENU },
  { "ESC", KEY_ESC },
  { "INSERT", KEY_INSERT },
  { "DELETE", KEY_DELETE },
  { "PAGE_UP", KEY_PAGE_UP },
  { "PAGE_DOWN", KEY_PAGE_DOWN },
  { "HOME", KEY_HOME },
  { "END", KEY_END },
  { "CAPS_LOCK", KEY_CAPS_LOCK },
  { "PRINT_SCREEN", KEY_PRINT_SCREEN },
  { "SCROLL_LOCK", KEY_SCROLL_LOCK },
  { "PAUSE", KEY_PAUSE },
  { "NUM_LOCK", KEY_NUM_LOCK },
  { "KP_SLASH", KEY_KP_SLASH },
  { "KP_ASTERISK", KEY_KP_ASTERISK },
  { "KP_MINUS", KEY_KP_MINUS },
  { "KP_PLUS", KEY_KP_PLUS },
  { "KP_ENTER", KEY_KP_ENTER },
  { "KP_1", KEY_KP_1 },
  { "KP_2", KEY_KP_2 },
  { "KP_3", KEY_KP_3 },
  { "KP_4", KEY_KP_4 },
  { "KP_5", KEY_KP_5 },
  { "KP_6", KEY_KP_6 },
  { "KP_7", KEY_KP_7 },
  { "KP_8", KEY_KP_8 },
  { "KP_9", KEY_KP_9 },
  { "KP_0", KEY_KP_0 },
  { "KP_DOT", KEY_KP_DOT },
  { "F1", KEY_F1 },
  { "F2", KEY_F2 },
  { "F3", KEY_F3 },
  { "F4", KEY_F4 },
  { "F5", KEY_F5 },
  { "F6", KEY_F6 },
  { "F7", KEY_F7 },
  { "F8", KEY_F8 },
  { "F9", KEY_F9 },
  { "F10", KEY_F10 },
  { "F11", KEY_F11 },
  { "F12", KEY_F12 },
  { "F13", KEY_F13 },
  { "F14", KEY_F14 },
  { "F15", KEY_F15 },
  { "F16", KEY_F16 },
  { "F17", KEY_F17 },
  { "F18", KEY_F18 },
  { "F19", KEY_F19 },
  { "F20", KEY_F20 },
  { "F21", KEY_F21 },
  { "F22", KEY_F22 },
  { "F23", KEY_F23 },
  { "F24", KEY_F24 },
};

const char LF = '\n';
const char CR = '\r';

// Jitter constants
const int KEY_JITTER_MAX_VALUE = 50;
const int DELAY_JITTER_MAX_VALUE = 100;

// Global variables
int keystrokeDelay = 0;  // Initial delay between keystrokes
int keyPressLength = 0;  // The time a key will be held down
int keyJitterMaxValue = KEY_JITTER_MAX_VALUE;
int delayJitterMaxValue = DELAY_JITTER_MAX_VALUE;
bool isDebugEnabled = true;
bool isConnected = false;
bool isEchoEnabled = true;
bool isStopRequested = false;
bool isPaused = false;
bool addKeyJitter = false;
bool addDelayJitter = false;
bool isBusy = false;
bool isInitialStatusShown = false;

// Function prototypes
void infoMessage(const String& message);
void debugMessage(const String& message);
void errorMessage(const String& message);
void keyPressWaitWithMinimum(int minimum);
void keyIntervalWaitWithMinimum(int minimum);
void waitWithMinimumValues(int minimum, int delayMillisSetting, int jitterMaxValue, bool addJitter);
void sendKey(const String& text, boolean hold);
void sendKeyCombination(String keys);
void releaseKey(const String& text);
void resetArduino();
void sendEditAction(char actionKey);
void cut();
void copy();
void paste();
void selectAll();
void handleEditCommand(const String& text);
CommandType parseCommandType(const String& command, String& parameter);
void handleCommand(const String& commandString);
void processCurrentAction();
String readSerialUntil(int maxLength);
void sendReturn();
void sendPingResponse();

void sendStatusResponse(bool fullStatus = false, UuidClearOption clearOption = NO_CLEAR);
void printHelp(bool sendToKeyboard = false);
bool isReady();

// Define ActionType enum and Action struct for non-blocking operations
enum ActionType {
  NONE,
  SEND_MESSAGE,
  TYPE_LOREM_IPSUM,
  TYPE_LOREM_LINES,
  SEND_KEY,
  SEND_COMBO,
  SEND_EDIT,
  HOLD_KEY,
  // Add other action types as needed
};

// Define Command structure before usage
struct UuidInfo {
  String uuid;
  bool isArtificial;
};

// Define an empty UuidInfo to use for resetting
UuidInfo emptyUuidInfo = { "", false };

// Global UUID variables for normal and priority commands
UuidInfo priorityUUID = { "", false };
UuidInfo normalUUID = { "", false };

String normalizeCommand(const String& command);

// Why crc24 ? Its to keep as many bits of the v8 generated uuid as random as possible but allow for a mostly increasing timestamp (over 2 weeks) and a hash of the board id
// the 24 bit parts are not intended to be univerally unique, just give some nice values for testing. Usually the UUID will be supplied by the calling PC program
// and will be a standard v4 uuid
uint32_t calculateCRC24(const String& input) {
  uint32_t crc = 0xB704CE;  // Initial value, can be adjusted based on polynomial
  const uint32_t polynomial = 0x864CFB;

  for (char c : input) {
    crc ^= (uint8_t)c << 16;  // Shift left to match 24-bit polynomial length

    for (int i = 0; i < 8; i++) {
      if (crc & 0x800000) {  // Check the 24th bit
        crc = (crc << 1) ^ polynomial;
      } else {
        crc <<= 1;
      }
    }
  }

  return crc & 0xFFFFFF;  // Mask to keep only the lower 24 bits
}

void seedRandomNumberGenerator() {
  // Read from spaced floating analog pins
  uint16_t analogValue1 = analogRead(A0);  // Read from analog pin A0
  delay(1);                                // Short delay to allow for ADC settling
  uint16_t analogValue2 = analogRead(A2);  // Read from analog pin A2
  delay(1);
  uint16_t analogValue3 = analogRead(A4);  // Read from analog pin A4

  // Get the current micros() value
  unsigned long timeValue = micros();  // Get the current time in microseconds

  // Combine the values to create a seed
  unsigned long seedValue = 0;
  seedValue ^= ((unsigned long)analogValue1 << 20);  // Shift and combine analogValue1
  seedValue ^= ((unsigned long)analogValue2 << 10);  // Shift and combine analogValue2
  seedValue ^= (unsigned long)analogValue3;          // Combine analogValue3 directly
  seedValue ^= timeValue;                            // XOR with micros() value

  // Seed the random number generator
  randomSeed(seedValue);
}

String generateUUIDv8() {
  // Compute CRC16 of the chip ID
  String chipId = getChipId();
  uint32_t crc24 = calculateCRC24(getChipId());

  // Seed the random number generator
  seedRandomNumberGenerator();

  // 24-bit timestamp in tenths of a second, rolls over every 14 days (1,209,600 seconds)
  uint32_t timeTenths = (millis() / 100UL) % 12096000UL;  // Rolls over every 14 days

  uint8_t uuid[16];

  // Initialize uuid with random bytes, Start from byte 6 as we override bytes 0-5
  for (int i = 6; i < 16; i++) {
    uuid[i] = random(0, 256);
  }
  // we then override certain bytes
  // Set bytes 0-2 to crc24 (24 bits)
  uuid[0] = (crc24 >> 16) & 0xFF;  // Bits 23-16
  uuid[1] = (crc24 >> 8) & 0xFF;   // Bits 15-8
  uuid[2] = crc24 & 0xFF;          // Bits 7-0

  // Set bytes 3-5 to time in tenths of seconds (24 bits)
  uuid[3] = (timeTenths >> 16) & 0xFF;  // Bits 23-16
  uuid[4] = (timeTenths >> 8) & 0xFF;   // Bits 15-8
  uuid[5] = timeTenths & 0xFF;          // Bits 7-0

  // Set version to 8 in the high 4 bits of byte 6
  uuid[6] = (uuid[6] & 0x0F) | 0x80;

  // Set variant to RFC 4122 in byte 8
  uuid[8] = (uuid[8] & 0x3F) | 0x80;  // Variant 1

  // Format UUID as a string with dashes using snprintf for clarity
  char uuidStr[37];  // 36 characters + null terminator
  snprintf(uuidStr, sizeof(uuidStr),
           "%02X%02X-%02X%02X%02X-%02X%02X-%02X%02X-%02X%02X%02X%02X%02X%02X",
           uuid[0], uuid[1], uuid[2], uuid[3], uuid[4], uuid[5], uuid[6], uuid[7],
           uuid[8], uuid[9], uuid[10], uuid[11], uuid[12], uuid[13], uuid[14], uuid[15]);

  // Return as a String object, which will auto-handle capitalisation
  return String(uuidStr);
}



// Retrieve unique chip ID for Arduino Due (SAM3X8E)
String getChipId() {
  uint32_t uid[4];
  uid[0] = *(uint32_t*)0x400E0740;  // REG_UID0 address
  uid[1] = *(uint32_t*)0x400E0744;  // REG_UID1 address
  uid[2] = *(uint32_t*)0x400E0748;  // REG_UID2 address
  uid[3] = *(uint32_t*)0x400E074C;  // REG_UID3 address

  String chipId = "";
  for (int i = 0; i < 4; i++) {
    String hexPart = String(uid[i], HEX);
    while (hexPart.length() < 8) {
      hexPart = "0" + hexPart;  // Pad with leading zeros
    }
    chipId += hexPart;
  }
  return chipId;
}

struct Action {
  ActionType type;
  String message;
  int index;
  int lineIndex;
  bool newLine;
  String parameter;
};

Action currentAction = { NONE };

void handleSecureTextCommand(const String& encodedText, const String& commandName) {
  // Decode the Base64 text
  String decodedText = base64Decode(encodedText);

  // Log only the UUID
  infoMessage("Typing " + commandName + " (UUID only logged).");
  debugMessage("UUID for " + commandName + " command: " + normalUUID.uuid);

  // Send the decoded text as keystrokes
  for (size_t i = 0; i < decodedText.length(); i++) {
    char currentChar = decodedText[i];

    // Handle newline characters explicitly
    if ((currentChar == '\r' && decodedText[i + 1] == '\n') || currentChar == '\n') {
      i++;  // Skip '\n' if '\r' was already processed
      Keyboard.press(KEY_RETURN);
      keyPressWaitWithMinimum(50);
      Keyboard.releaseAll();
      keyIntervalWaitWithMinimum(50);
    } else {
      // Send regular characters
      Keyboard.press(currentChar);
      keyPressWaitWithMinimum(10);
      Keyboard.releaseAll();
      keyIntervalWaitWithMinimum(10);
    }
  }
}



void setup() {
  delay(150);
  // Start the serial communication
  Serial.begin(115200);
  Keyboard.begin();
  isConnected = true;
  // Wait for a stable connection
  // Wait for Serial connection with a 2-second timeout
  unsigned long startTime = millis();
  while (!Serial && (millis() - startTime < 2000)) {
    // Wait until Serial is connected or timeout of 2 seconds is reached
  }
  Serial.println("");
  infoMessage("Keyboard emulation started");
  infoMessage("HELP command will print help.");
}

void restart() {
  WDT->WDT_MR = WDT_MR_WDDIS;  // Disable the watchdog if enabled
  // Enable the watchdog with the reset system and very short timeout (1ms)
  WDT->WDT_MR = WDT_MR_WDV(0xFFF) | WDT_MR_WDRSTEN | WDT_MR_WDD(0xFFF) | WDT_MR_WDDBGHLT | WDT_MR_WDIDLEHLT;
}

void loop() {

  if (!isInitialStatusShown) {
    // Send ready message after setup is complete, This is a good place to send the full status
    sendStatusResponse(true);
    isInitialStatusShown = true;
  }

  // Check for serial input
  if (Serial.available() > 0) {
    String command = readSerialUntil(4096);  // Read until newline character or max chars
    if (command.length() > 0) {
      infoMessage("Command: " + command);
      handleCommand(command);
    }
  }

  // Process the current action incrementally
  processCurrentAction();
}

String escapeForJson(const String& input) {
  String output = "";
  for (size_t i = 0; i < input.length(); i++) {
    char c = input[i];
    switch (c) {
      case '\"': output += "\\\""; break;
      case '\\': output += "\\\\"; break;
      case '\n': output += "\\n"; break;
      case '\r': output += "\\r"; break;
      case '\t': output += "\\t"; break;
      default: output += c; break;
    }
  }
  return output;
}

// Implementations of functions

void infoMessage(const String& message) {
  Serial.print(F("{\"level\": \"INFO\", \"message\": \""));
  Serial.print(escapeForJson(message));

  // Include UUIDs if they’re set, with "artificialId" if applicable
  if (priorityUUID.uuid) {
    Serial.print(F("\", \"priorityUUID\": \""));
    Serial.print(priorityUUID.uuid);
    if (priorityUUID.isArtificial) {
      Serial.print(F("\", \"artificialId\": true"));
    }
  }
  if (normalUUID.uuid) {
    Serial.print(F("\", \"commandUUID\": \""));
    Serial.print(normalUUID.uuid);
    if (normalUUID.isArtificial) {
      Serial.print(F("\", \"artificialId\": true"));
    }
  }

  Serial.println(F("\"}"));
}

void debugMessage(const String& message) {
  if (isDebugEnabled) {
    Serial.print(F("{\"level\": \"DEBUG\", \"message\": \""));
    Serial.print(escapeForJson(message));

    // Include UUIDs if they’re set, with "artificialId" if applicable
    if (priorityUUID.uuid) {
      Serial.print(F("\", \"priorityUUID\": \""));
      Serial.print(priorityUUID.uuid);
      if (priorityUUID.isArtificial) {
        Serial.print(F("\", \"artificialId\": true"));
      }
    }
    if (normalUUID.uuid) {
      Serial.print(F("\", \"commandUUID\": \""));
      Serial.print(normalUUID.uuid);
      if (normalUUID.isArtificial) {
        Serial.print(F("\", \"artificialId\": true"));
      }
    }

    Serial.println(F("\"}"));
  }
}

void errorMessage(const String& message) {
  Serial.print(F("{\"level\": \"ERROR\", \"message\": \""));
  Serial.print(escapeForJson(message));

  // Include UUIDs if they’re set, with "artificialId" if applicable
  if (priorityUUID.uuid) {
    Serial.print(F("\", \"priorityUUID\": \""));
    Serial.print(priorityUUID.uuid);
    if (priorityUUID.isArtificial) {
      Serial.print(F("\", \"artificialId\": true"));
    }
  }
  if (normalUUID.uuid) {
    Serial.print(F("\", \"commandUUID\": \""));
    Serial.print(normalUUID.uuid);
    if (normalUUID.isArtificial) {
      Serial.print(F("\", \"artificialId\": true"));
    }
  }

  Serial.println(F("\"}"));
}

void keyPressWaitWithMinimum(int minimum) {
  waitWithMinimumValues(minimum, keyPressLength, keyJitterMaxValue, addKeyJitter);
}

void keyIntervalWaitWithMinimum(int minimum) {
  waitWithMinimumValues(minimum, keystrokeDelay, delayJitterMaxValue, addDelayJitter);
}

int clamp(int value, int min, int max) {
  return value < min ? min : (value > max ? max : value);
}

/**
 * Wait with minimum values and optional jitter
 */
void waitWithMinimumValues(int minimum, int delayMillisSetting, int jitterMaxValue, bool addJitter) {
  int delayMillis = delayMillisSetting;
  if (addJitter && jitterMaxValue > 0) {
    if (delayMillisSetting == 0) {
      delayMillis = jitterMaxValue + minimum;
    }
    int jitter = random(0, jitterMaxValue + 1);
    if (random(0, 2) == 0) {  // Randomly decide to add or subtract
      delayMillis += jitter;
    } else {
      delayMillis -= jitter;
    }
    // if (delayMillis < 0) {
    //   delayMillis = 0;  // Ensure delayMillis is not negative
    // }
    delayMillis = clamp(delayMillis, minimum, jitterMaxValue + minimum);
  }
  if (delayMillis < minimum) delayMillis = minimum;
  if (addJitter) debugMessage("Delay applied: " + String(delayMillis));
  delay(delayMillis);
}


// check if a command is a priority command
bool isPriorityCommand(CommandType type) {
  return type == CMD_STOP || type == CMD_PAUSE || type == CMD_RESUME || type == CMD_RESET;
}

// Function to find a command and its parameter from the normalized command string
Command* findCommand(const String& command, String& parameter) {
  String normalizedCommand = normalizeCommand(command);  // Normalize the command if needed

  // Iterate through knownCommands[] to find the longest matching command
  Command* bestMatch = nullptr;

  for (Command& cmd : knownCommands) {
    if (normalizedCommand.startsWith(cmd.name)) {
      if (!bestMatch || strlen(cmd.name) > strlen(bestMatch->name)) {
        bestMatch = &cmd;                                          // Update to the best match found
        parameter = command.substring(String(cmd.name).length());  // Extract parameter if expected
      }
    }
  }

  return bestMatch;  // Return the longest match found, or nullptr if no match
}


void processCurrentAction() {
  // If no action is currently being processed or the system is stopped or paused
  if (currentAction.type == NONE || isStopRequested || isPaused) {
    // If the system was busy, but now it's free, update the status once
    if (isBusy) {
      isBusy = false;
      sendStatusResponse(false, BOTH);  // Notify that the device is now free
    }
    return;
  }

  // If the system is not busy and is starting an action
  if (!isBusy) {
    isBusy = true;         // Mark as busy when starting a new action
    sendStatusResponse();  // Notify that the device is now busy
  }

  // Process the current action
  switch (currentAction.type) {
    case SEND_MESSAGE:
      if (currentAction.index < currentAction.message.length()) {
        char currentChar = currentAction.message[currentAction.index];
        if (isStopRequested) {
          Keyboard.releaseAll();
          currentAction.type = NONE;
        } else {
          Keyboard.press(currentChar);
          keyPressWaitWithMinimum(10);
          Keyboard.releaseAll();
          keyIntervalWaitWithMinimum(10);
          currentAction.index++;
        }
      } else {
        if (currentAction.newLine) sendReturn();
        currentAction.type = NONE;
      }
      break;

    case TYPE_LOREM_IPSUM:
    case TYPE_LOREM_LINES:
      {
        const char* loremIpsumLines[] = {
          "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ",
          "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. ",
          "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. ",
          "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
        };
        int numLines = sizeof(loremIpsumLines) / sizeof(loremIpsumLines[0]);
        if (currentAction.lineIndex < numLines) {
          if (currentAction.index < strlen(loremIpsumLines[currentAction.lineIndex])) {
            char currentChar = loremIpsumLines[currentAction.lineIndex][currentAction.index];
            if (isStopRequested) {
              Keyboard.releaseAll();
              currentAction.type = NONE;
            } else {
              Keyboard.press(currentChar);
              keyPressWaitWithMinimum(10);
              Keyboard.releaseAll();
              keyIntervalWaitWithMinimum(10);
              currentAction.index++;
            }
          } else {
            if (currentAction.type == TYPE_LOREM_LINES) {
              sendReturn();
            }
            currentAction.lineIndex++;
            currentAction.index = 0;
          }
        } else {
          currentAction.type = NONE;
        }
        break;
      }

    case SEND_KEY:
      if (currentAction.index == 0) {
        sendKey(currentAction.parameter, false);
        currentAction.index++;
      } else {
        currentAction.type = NONE;
      }
      break;

    case SEND_COMBO:
      if (currentAction.index == 0) {
        sendKeyCombination(currentAction.parameter);
        currentAction.index++;
      } else {
        currentAction.type = NONE;
      }
      break;

    case SEND_EDIT:
      if (currentAction.index == 0) {
        handleEditCommand(currentAction.parameter);
        currentAction.index++;
      } else {
        currentAction.type = NONE;
      }
      break;

    case HOLD_KEY:
      if (currentAction.index == 0) {
        sendKey(currentAction.parameter, true);
        currentAction.index++;
      } else {
        currentAction.type = NONE;
      }
      break;

    default:
      currentAction.type = NONE;
      break;
  }

  // If the action is done and system was busy, mark it as free and update the status
  if (currentAction.type == NONE && isBusy) {
    isBusy = false;
    sendStatusResponse();  // Notify that the device is now free
  }
}


void sendReturn() {
  Keyboard.press('\n');
  keyPressWaitWithMinimum(50);  // Delay before newline (optional, adjust as needed)
  Keyboard.releaseAll();
  keyIntervalWaitWithMinimum(50);
}

void sendPingResponse() {
  Serial.println("RESPONSE:OK");  // Indicate that the command was received and processed successfully
}

void sendStatusResponse(bool fullStatus, UuidClearOption clearOption) {
  if (fullStatus) {
    // Full detailed status when requested
    Serial.print(F("RESPONSE:{\"app\": \"RoboKeysDuino\", \"status\": \""));
    Serial.print(isConnected ? "connected" : "disconnected");
    Serial.print(F("\", \"ready\": \""));
    Serial.print(isReady() ? "yes" : "no");
    Serial.print(F("\", \"paused\": \""));
    Serial.print(isPaused ? "yes" : "no");
    Serial.print(F("\", \"debug\": \""));
    Serial.print(isDebugEnabled ? "on" : "off");
    Serial.print(F("\", \"echo\": \""));
    Serial.print(isEchoEnabled ? "on" : "off");
    Serial.print(F("\", \"output\": \""));
    Serial.print(isStopRequested ? "off" : "on");
    Serial.print(F("\", \"keyJitter\": \""));
    Serial.print(addKeyJitter ? "on" : "off");
    Serial.print(F("\", \"delayJitter\": \""));
    Serial.print(addDelayJitter ? "on" : "off");

    // Append optional fields only if they are non-zero
    Serial.print(F(", \"KeyStrokeDelay\": "));
    Serial.print(keystrokeDelay);
    Serial.print(F(", \"keyPressLength\": "));
    Serial.print(keyPressLength);

    Serial.print(F(", \"keyJitterMaxValue\": "));
    Serial.print(keyJitterMaxValue);

    Serial.print(F(", \"delayJitterMaxValue\": "));
    Serial.print(delayJitterMaxValue);

    // Include UUIDs if they’re set
    // Include UUIDs if they’re set, with "artificialId" if applicable
    if (priorityUUID.uuid) {
      Serial.print(F("\", \"priorityUUID\": \""));
      Serial.print(priorityUUID.uuid);
      if (priorityUUID.isArtificial) {
        Serial.print(F("\", \"artificialId\": true"));
      }
    }
    if (normalUUID.uuid) {
      Serial.print(F("\", \"commandUUID\": \""));
      Serial.print(normalUUID.uuid);
      if (normalUUID.isArtificial) {
        Serial.print(F("\", \"artificialId\": true"));
      }
    }

    // Include a message based on the ready state
    Serial.print(F(", \"message\": \""));
    Serial.print(isReady() ? "Ready for commands." : "Not ready - only priority commands accepted.");

    // Version information
    Serial.print(F(", \"version\": \"0.6.0\""));
    Serial.println(F("\"}"));

  } else {
    // Minimal status, similar to the full status, but for basic updates
    Serial.print(F("RESPONSE:{\"app\": \"RoboKeysDuino\", \"paused\": \""));
    Serial.print(isPaused ? "yes" : "no");
    Serial.print(F("\", \"busy\": \""));
    Serial.print(isBusy ? "yes" : "no");
    // Include UUIDs if they’re set, with artificialId if applicable
    if (priorityUUID.uuid) {
      Serial.print(F("\", \"priorityUUID\": \""));
      Serial.print(priorityUUID.uuid);
      if (priorityUUID.isArtificial) {
        Serial.print(F("\", \"artificialId\": true"));
      }
    }
    if (normalUUID.uuid) {
      Serial.print(F("\", \"commandUUID\": \""));
      Serial.print(normalUUID.uuid);
      if (normalUUID.isArtificial) {
        Serial.print(F("\", \"artificialId\": true"));
      }
    }

    Serial.println(F("\"}"));
  }

  // Clear UUIDs based on the clearOption enum
  switch (clearOption) {
    case PRIORITY:
      priorityUUID = emptyUuidInfo;
      break;
    case NORMAL:
      normalUUID = emptyUuidInfo;
      break;
    case BOTH:
      priorityUUID = emptyUuidInfo;
      normalUUID = emptyUuidInfo;
      break;
    case NONE:
    default:
      break;
  }
}

String normalizeCommand(const String& command) {
  const int MAX_COMMAND_LENGTH = 32;
  char buffer[MAX_COMMAND_LENGTH + 1];  // Buffer for 32 characters + null terminator

  // Copy up to the first 32 characters of the command into the buffer
  command.toCharArray(buffer, MAX_COMMAND_LENGTH + 1);  // Copy with a null terminator

  // Normalize the command in place (replace _ - . , space with :)
  for (int i = 0; i < MAX_COMMAND_LENGTH && buffer[i] != '\0'; i++) {
    if (buffer[i] == '_' || buffer[i] == ' ' || buffer[i] == '-' || buffer[i] == '.' || buffer[i] == ',') {
      buffer[i] = ':';  // Replace delimiters with colons
    }
    buffer[i] = toupper(buffer[i]);  // Convert to uppercase
  }

  // Convert the normalized buffer back to a String and return it
  return String(buffer);
}


bool isReady() {
  return !isBusy && !isStopRequested;
}

CommandType parseCommandType(const String& command, String& parameter) {
  debugMessage("<" + command + ">");

  // Ensure the command is normalized
  String normalizedCommand = normalizeCommand(command);

  // Check each known command for a match
  for (Command cmd : knownCommands) {
    if (normalizedCommand.startsWith(cmd.name)) {
      // If the command expects a parameter, extract it
      if (cmd.hasParameter) {
        parameter = command.substring(String(cmd.name).length());  // Extract parameter
        parameter.trim();                                          // Trim spaces
      }
      return cmd.type;
    }
  }

  return INVALID;  // No match found
}



void sendKey(const String& text, boolean hold) {

  String upperCaseText = text;
  upperCaseText.toUpperCase();
  Serial.println("Keyboard key send requested : " + text);
  uint8_t keyCode = findKeyCode(upperCaseText);
  if (keyCode != 0) {
    debugMessage("Key code found : " + upperCaseText);
    if (isStopRequested) {
      Keyboard.releaseAll();
      delay(10);
      return;
    }
    Keyboard.press(keyCode);  // Press the key
    keyPressWaitWithMinimum(50);
    if (!hold) {                  // Optional delay to ensure key press is registered
      Keyboard.release(keyCode);  // Release the key
      keyIntervalWaitWithMinimum(50);
      debugMessage("key released");
    }
  }
}

void sendKeyCombination(String keys) {

  String lowerCaseModifiers = keys;
  lowerCaseModifiers.toLowerCase();

  // Split the keys string into individual key components
  int indexOfCtrl = lowerCaseModifiers.indexOf("ctrl");
  if (indexOfCtrl == -1) indexOfCtrl = lowerCaseModifiers.indexOf("@c");
  int indexOfAlt = lowerCaseModifiers.indexOf("alt");
  if (indexOfAlt == -1) indexOfAlt = lowerCaseModifiers.indexOf("@a");
  int indexOfAltGr = lowerCaseModifiers.indexOf("altgr");
  int indexOfShift = lowerCaseModifiers.indexOf("shift");
  if (indexOfShift == -1) indexOfShift = lowerCaseModifiers.indexOf("@s");
  int indexOfGui = lowerCaseModifiers.indexOf("gui");
  int indexOfKey = lowerCaseModifiers.indexOf("-");

  if (indexOfCtrl == -1 && indexOfAlt == -1 && indexOfShift == -1 && indexOfGui == -1 && indexOfKey == -1 && indexOfAltGr == -1) {
    debugMessage("No modifier key");
  }

  // Check if each modifier is present in the keys string and press accordingly
  if (indexOfCtrl != -1) {
    Keyboard.press(KEY_LEFT_CTRL);
    keyPressWaitWithMinimum(10);
    debugMessage("CTRL held down");
  }
  if (indexOfAlt != -1) {
    Keyboard.press(KEY_LEFT_ALT);
    keyPressWaitWithMinimum(10);
    debugMessage("ALT held down");
  }
  if (indexOfShift != -1) {
    Keyboard.press(KEY_LEFT_SHIFT);
    keyPressWaitWithMinimum(10);
    debugMessage("SHIFT held down");
  }
  if (indexOfGui != -1) {
    Keyboard.press(KEY_LEFT_GUI);
    keyPressWaitWithMinimum(10);
    debugMessage("GUI held down");
  }
  if (indexOfAltGr != -1) {
    Keyboard.press(KEY_RIGHT_ALT);
    keyPressWaitWithMinimum(10);
    debugMessage("ALT-GR held down");
  }

  // Press the specified key
  String mainKey = keys.substring(indexOfKey + 1);
  if (mainKey.startsWith("f")) {
    // If the key is a function key (F1-F24)
    int fKeyNumber = mainKey.substring(1).toInt();
    if (fKeyNumber >= 1 && fKeyNumber <= 24) {
      Keyboard.press(KEY_F1 + fKeyNumber - 1);  // Convert F1-F24 to corresponding key code
      debugMessage("Function key pressed.");
    }
  } else {
    // If the key is not a function key, assume it's a normal key
    for (int i = 0; i < mainKey.length(); i++) {
      char currentChar = mainKey[i];
      if (isStopRequested) {
        Keyboard.releaseAll();
        delay(10);
        return;
      }
      Keyboard.press(currentChar);
      delay(10);
    }
    debugMessage("Normal key press.");
  }
  keyPressWaitWithMinimum(10);
  // Release all keys
  Keyboard.releaseAll();
  keyIntervalWaitWithMinimum(10);
}

void releaseKey(const String& text) {

  String upperCaseText = text;
  upperCaseText.toUpperCase();
  debugMessage("Keyboard key release requested : " + upperCaseText);
  uint8_t keyCode = findKeyCode(upperCaseText);
  // Optional delay to ensure key press is registered
  Keyboard.release(keyCode);  // Release the key
  keyIntervalWaitWithMinimum(50);
  debugMessage("key released");
}

void resetArduino() {
  // Reset the Arduino Due
  //NVIC_SystemReset();
  restart();
}

void sendEditAction(char actionKey) {

  Keyboard.press(KEY_LEFT_CTRL);
  keyPressWaitWithMinimum(10);
  Keyboard.press(actionKey);
  keyPressWaitWithMinimum(50);
  Keyboard.releaseAll();
  keyIntervalWaitWithMinimum(50);
}

void cut() {
  sendEditAction('x');
  debugMessage("cut sent");
}

void copy() {
  sendEditAction('c');
  debugMessage("copy sent");
}

void paste() {
  sendEditAction('v');
  debugMessage("paste sent");
}

void selectAll() {
  sendEditAction('a');
  debugMessage("Select all sent");
}

void handleEditCommand(const String& text) {
  debugMessage("Handling edit command");
  String command = text;
  command.toLowerCase();
  if (command == "cut") {
    cut();
  } else if (command == "copy") {
    copy();
  } else if (command == "paste") {
    paste();
  } else if (command == "selectall" || command == "select:all" || command == "select-all") {
    selectAll();
  } else {
    debugMessage("A bad edit command was given");
    // Handle invalid command
  }
}

uint8_t findKeyCode(const String& text) {
  for (size_t i = 0; i < sizeof(keyMappings) / sizeof(keyMappings[0]); i++) {
    if (text.equals(keyMappings[i].text)) {
      debugMessage("Key found: " + text + ", keyCode: " + String(keyMappings[i].keyCode));
      return keyMappings[i].keyCode;
    }
  }
  debugMessage("Key not found: " + text);
  // Return 0 if key not found
  return 0;
}

String jsonEscape(const String& input) {
  String escaped = "\"";  // Start with a double quote for JSON compatibility
  for (unsigned int i = 0; i < input.length(); i++) {
    char c = input[i];
    switch (c) {
      case '\"': escaped += "\\\""; break;
      case '\\': escaped += "\\\\"; break;
      case '\b': escaped += "\\b"; break;
      case '\f': escaped += "\\f"; break;
      case '\n': escaped += "\\n"; break;
      case '\r': escaped += "\\r"; break;
      case '\t': escaped += "\\t"; break;
      default:
        if (c < 0x20 || c > 0x7E) {
          // Convert non-printable characters to Unicode escape sequence
          char buffer[7];
          snprintf(buffer, sizeof(buffer), "\\u%04X", c);
          escaped += buffer;
        } else {
          escaped += c;
        }
    }
  }
  escaped += "\"";  // End with a double quote
  return escaped;
}

void processCommand(const String& commandString) {
  if (commandString.length() > 0) {
    String parameter;                                      // Holds the command parameter if any
    Command* cmd = findCommand(commandString, parameter);  // Find the command

    if (!cmd) {
      Serial.println("Invalid command: " + jsonEscape(commandString.substring(32)));
      return;
    } else {
      Serial.println("Processing command: " + jsonEscape(commandTypeToString(cmd->type)));
    }
    // If the Arduino is busy, only allow priority commands

    // Priority commands check when busy or paused
    if (isPaused && cmd->type != CMD_RESUME && cmd->type != CMD_STOP && cmd->type != CMD_RESET) {
      sendStatusResponse();
      return;
    }

    if (isBusy && cmd->type != CMD_STOP && cmd->type != CMD_PAUSE && cmd->type != CMD_RESUME) {
      sendStatusResponse();
      return;
    }
    // Declare variable outside of the switch statement
    int delayValue = 0;

    switch (cmd->type) {
      case HELP:
        printHelp();
        break;
      case TYPE_HELP:
        printHelp(true);
        break;
      case PING:
        sendPingResponse();
        break;
      case STATUS:
        sendStatusResponse(true, NORMAL);
        break;
      case LOREM:
        isStopRequested = false;  // Reset stop flag
        currentAction.type = TYPE_LOREM_IPSUM;
        currentAction.index = 0;
        currentAction.lineIndex = 0;
        currentAction.newLine = false;
        break;
      case LOREM_LINES:
        isStopRequested = false;  // Reset stop flag
        currentAction.type = TYPE_LOREM_LINES;
        currentAction.index = 0;
        currentAction.lineIndex = 0;
        currentAction.newLine = true;
        break;
      case TEXT:
        isStopRequested = false;  // Reset stop flag
        currentAction.type = SEND_MESSAGE;
        currentAction.message = parameter;
        currentAction.index = 0;
        currentAction.newLine = false;
        break;
      case LINE:
        isStopRequested = false;  // Reset stop flag
        currentAction.type = SEND_MESSAGE;
        currentAction.message = parameter;
        currentAction.index = 0;
        currentAction.newLine = true;
        break;
      case KEY:
        isStopRequested = false;  // Reset stop flag
        currentAction.type = SEND_KEY;
        currentAction.parameter = parameter;
        currentAction.index = 0;
        break;
      case COMBO:
        isStopRequested = false;  // Reset stop flag
        currentAction.type = SEND_COMBO;
        currentAction.parameter = parameter;
        currentAction.index = 0;
        break;
      case EDIT:
        isStopRequested = false;  // Reset stop flag
        currentAction.type = SEND_EDIT;
        currentAction.parameter = parameter;
        currentAction.index = 0;
        break;
      case HOLD:
        isStopRequested = false;  // Reset stop flag
        currentAction.type = HOLD_KEY;
        currentAction.parameter = parameter;
        currentAction.index = 0;
        break;
      case CMD_STOP:
        isStopRequested = true;
        currentAction.type = NONE;  // Clear the current action
        isBusy = false;
        Keyboard.releaseAll();  // Release any pressed keys
        sendStatusResponse(false, BOTH);
        debugMessage("Stop command received. Current action stopped and cleared.");
        break;
      case CMD_PAUSE:
        isPaused = true;
        //   Keyboard.releaseAll();  // Release any pressed keys
        isBusy = false;
        sendStatusResponse(false, PRIORITY);
        debugMessage("Pause command received. Typing paused.");
        break;
      case CMD_RESUME:
        isPaused = false;
        isStopRequested = false;  // Reset the stop flag here
        debugMessage("Resume command received. Typing resumed. Stop flag cleared.");
        sendStatusResponse(false, PRIORITY);
        break;
      case RELEASE_KEY:
        releaseKey(parameter);
        break;
      case RELEASE_ALL_KEYS:
        Keyboard.releaseAll();
        delay(50);
        break;
      case CMD_OUTPUT_OFF:
        isStopRequested = true;
        break;
      case CMD_OUTPUT_ON:
        isStopRequested = false;
        break;
      case CMD_SET_KEY_JITTER_VALUE:
        keyJitterMaxValue = commandString.substring(23).toInt();  // Extract delay value from command
        debugMessage("Maximum key jitter value set to +/- " + String(keyJitterMaxValue) + " milliseconds");
        break;
      case CMD_SET_DELAY_JITTER_VALUE:
        delayJitterMaxValue = commandString.substring(25).toInt();  // Extract delay value from command
        debugMessage("Maximum delay jitter value set to +/- " + String(delayJitterMaxValue) + " milliseconds");
        break;
      case CMD_SET_DELAY:
        delayValue = commandString.substring(14).toInt();  // Extract delay value from command
        keystrokeDelay = delayValue;
        debugMessage("Additional Keystroke delay set to " + String(keystrokeDelay) + " milliseconds");
        break;
      case CMD_SET_PRESS_LENGTH:
        keyPressLength = commandString.substring(21).toInt();  // Extract delay value from command
        debugMessage("Additional keypress time set to " + String(keyPressLength) + " milliseconds");
        break;
      case CMD_RESET:
        // Handle reset command
        // Handle reset command by first sending a stop command
        isStopRequested = true;
        currentAction.type = NONE;        // Clear the current action
        Keyboard.releaseAll();            // Release any pressed keys
        isBusy = false;                   // Ensure the device is not busy
        sendStatusResponse(false, BOTH);  // Notify that the device is stopping
        delay(1000);                      // Give time to process stop before reset
        // Perform the reset after stop
        resetArduino();
        break;
      case CMD_ECHO_ON:
        isEchoEnabled = true;
        break;
      case CMD_ECHO_OFF:
        isEchoEnabled = false;
        break;
      case CMD_DEBUG_ON:
        isDebugEnabled = true;
        break;
      case CMD_DEBUG_OFF:
        isDebugEnabled = false;
        break;
      case CMD_JITTER_ON:
        addKeyJitter = true;
        addDelayJitter = true;
        break;
      case CMD_JITTER_OFF:
        addKeyJitter = false;
        addDelayJitter = false;
        break;
      case CMD_KEY_JITTER_ON:
        addKeyJitter = true;
        break;
      case CMD_KEY_JITTER_OFF:
        addKeyJitter = false;
        break;
      case CMD_DELAY_JITTER_ON:
        addDelayJitter = true;
        break;
      case CMD_DELAY_JITTER_OFF:
        addDelayJitter = false;
        break;
      case PASSWORD:
        // This is a sensitive command so its all kept here and not split into an action
        if (parameter.length() > 0) {
          handleSecureTextCommand(parameter, "password");
          sendReturn();
        } else {
          errorMessage("Password command missing parameter.");
        }
        break;
      case PRIVATE_TEXT:
        // This is a sensitive command so its all kept here and not split into an action
        if (parameter.length() > 0) {
          handleSecureTextCommand(parameter, "private-text");
        } else {
          errorMessage("private-text command missing parameter.");
        }
        break;
      case CMD_CONNECT:
        // Handle connect command
        if (!isConnected) {
          Keyboard.begin();
          debugMessage("Keyboard is now connected");
        } else {
          isConnected = true;  // Simulate keyboard connection
          debugMessage("Keyboard already connected");
        }
        break;
      case CMD_DISCONNECT:
        // Handle disconnect command
        Keyboard.end();
        isConnected = false;  // Simulate keyboard disconnection
        debugMessage("Keyboard is now disconnected");
        break;
      case CMD_RECONNECT:
        // Disconnect the keyboard
        isConnected = false;  // Update status flag to show disconnection
        debugMessage("Keyboard disconnected.");
        Keyboard.end();
        delay(500);  // Brief delay to allow the host to register the disconnect

        // Reconnect the keyboard
        Keyboard.begin();
        delay(500);          // Brief delay to allow the connection to establish
        isConnected = true;  // Update status flag to show connection
        debugMessage("Keyboard reconnected.");
        break;

      case INVALID:
        // Handle invalid command
        Serial.print("Invalid command -> ");
        Serial.println(commandString);
        break;
    }

    // Reset priorityUUID if the command is priority, otherwise reset normalUUID
    if (isPriorityCommand(cmd->type)) {
      priorityUUID = emptyUuidInfo;
    } else {
      normalUUID = emptyUuidInfo;
    }
  }
}


// Step 3: Adjust the handleCommand function to store the UUID accordingly
void handleCommand(const String& commandString) {
  String uuid;
  CommandType commandType;
  String actualCommand;
  bool startsWithId = false;

  String trimmedCommand = commandString;
  trimmedCommand.trim();  // Trim in place
  if (trimmedCommand.length() == 0) {
    Serial.println("Ignoring empty command.");
    return;
  }

  if (commandString.length() >= 3) {
    String strLower = commandString.substring(0, 3);
    strLower.toLowerCase();
    if (strLower.startsWith("id=")) {
      startsWithId = true;
    }
  }
  if (startsWithId) {  // Command has a UUID
    int commaIndex = commandString.indexOf(',');
    uuid = commandString.substring(3, commaIndex);  // Extract UUID
    actualCommand = commandString.substring(commaIndex + 1);
  } else {  // No UUID, generate a new v8 UUID
    uuid = generateUUIDv8();
    actualCommand = commandString;
  }

  String parameter;
  commandType = parseCommandType(actualCommand, parameter);

  // Store generated UUID in the correct global based on command priority
  if (isPriorityCommand(commandType)) {
    priorityUUID.uuid = uuid.c_str();  // Assign the C-string to the uuid field
    priorityUUID.isArtificial = true;
  } else {
    normalUUID.uuid = uuid.c_str();  // Assign the C-string to the uuid field
    normalUUID.isArtificial = true;
  }

  processCommand(actualCommand);  // Process the command
}

String applyUnnormalizedChars(const String& normalized, const String& raw) {
  String redactedCommand = "";  // Result after replacing colons with original chars

  for (size_t i = 0; i < normalized.length(); ++i) {
    char normalizedChar = normalized.charAt(i);
    char rawChar = raw.charAt(i);

    if (normalizedChar == ':') {
      // Replace colon with the corresponding raw character
      redactedCommand += rawChar;
    } else {
      // Keep the normalized character as is
      redactedCommand += normalizedChar;
    }
  }

  return redactedCommand;
}

String readSerialUntil(int maxLength) {
  String rawInputString = "";         // Original input string
  String normalizedInputString = "";  // Normalized input
  bool started = false;

  while (true) {
    if (Serial.available() > 0) {
      char incomingChar = Serial.read();
      rawInputString += incomingChar;

      // Check for line-ending characters
      if (incomingChar == LF || incomingChar == CR) {
        break;
      }

      if (!started && !isAlphaNumeric(incomingChar)) {
        continue;
      }
      started = true;

      char normalizedChar = incomingChar;

      // Normalize character
      if (normalizedChar == '_' || normalizedChar == ' ' || normalizedChar == '-' || normalizedChar == '.' || normalizedChar == ',') {
        normalizedChar = ':';  // Replace with colon
      }
      normalizedChar = toupper(normalizedChar);  // Convert to uppercase
      normalizedInputString += normalizedChar;

      if (normalizedInputString.endsWith("PASSWORD:")) {
        Serial.println("Detected sensitive command. Redacting...");
        String redactedNormalized = normalizedInputString + "<VALUE REDACTED>";
        String finalRedacted = applyUnnormalizedChars(redactedNormalized, rawInputString);
        Serial.println("Redacted Command: " + finalRedacted);
        break;
      }
      else if (normalizedInputString.endsWith("PRIVATE:")) {
        Serial.println("Detected sensitive command. Redacting...");
        String redactedNormalized = normalizedInputString + "<VALUE REDACTED>";
        String finalRedacted = applyUnnormalizedChars(redactedNormalized, rawInputString);
        Serial.println("Redacted Private text: " + finalRedacted);
        break;
      }

      if (rawInputString.length() >= maxLength) {
        break;
      }
    }
  }

  rawInputString.trim();  // Trim in place
  if (rawInputString.length() == 0) {
    Serial.println("No valid input received.");
    return "";  // Return empty string for invalid input
  }

  Serial.println("Raw Input: " + rawInputString);
  Serial.println("Normalized Input: " + normalizedInputString);

  return rawInputString;  // Return the raw input
}

void printHelp(bool sendToKeyboard) {
  // Define the help message
  const char* helpMessage[] = {
    "",
    "ROBO-KEYBOARD",
    "-------------",
    "This uses an Arduino Due to act as a virtual keyboard, that can be used for keyboard macros or testing.",
    "",
    "Usage:",
    "  HELP - Send this help text to the serial port.",
    "  TYPE_HELP - Send this help text to the keyboard.",
    "  PING - Check if the device is responsive",
    "  STATUS - Get current status of the device",
    "  LOREM - Type the Lorem Ipsum text on the keyboard",
    "  LOREM_LINES - Type the Lorem Ipsum text on the keyboard, as 4 lines of text",
    "  TEXT:<message> - Send a message as keystrokes",
    "  LINE:<message> - Send a message followed by a newline",
    "  KEY:<key> - Send a single key press",
    "  COMBO:<keys> - Send a combination of keys (e.g., CTRL-ALT-DEL)",
    "  EDIT:<action> - Perform an edit action (cut, copy, paste, selectall)",
    "  HOLD:<key> - Hold a key down",
    "  RELEASE_KEY:<key> - Release a specific key",
    "  RELEASE:ALL - Release all held keys",
    "  CMD:OUTPUT:OFF - Stop sending keys to the keyboard",
    "  CMD:OUTPUT:ON - Resume sending keys to the keyboard",
    "  CMD:SET:DELAY:<value> - Set delay between keystrokes",
    "  CMD:SET:PRESS-LENGTH:<value> - Set the length of key presses",
    "  CMD:RESET - Reset the device",
    "  CMD:ECHO:ON - Enable echoing of serial input",
    "  CMD:ECHO:OFF - Disable echoing of serial input",
    "  CMD:DEBUG:ON - Enable debug messages",
    "  CMD:DEBUG:OFF - Disable debug messages",
    "  CMD:JITTER:ON - Enable jitter for both key press and release timings",
    "  CMD:JITTER:OFF - Disable jitter for both key press and release timings",
    "  CMD:KEY_JITTER:ON - Enable jitter for key press timings only",
    "  CMD:KEY_JITTER:OFF - Disable jitter for key press timings only",
    "  CMD:DELAY_JITTER:ON - Enable jitter for key interval timings only",
    "  CMD:DELAY_JITTER:OFF - Disable jitter for key interval timings only",
    "  CMD:SET:KEY_JITTER_MAX:<value> - Set maximum key press jitter value",
    "  CMD:SET:DELAY_JITTER_MAX:<value> - Set maximum delay after keypress jitter value",
    "  CMD:CONNECT - Connect the keyboard",
    "  CMD:DISCONNECT - Disconnect the keyboard",
    "  CMD:RECONNECT - Reconnect the keyboard",
    "  CMD:STOP - Stop ongoing actions and clear pending text",
    "  CMD:PAUSE - Pause ongoing actions without losing data.",
    "  CMD:RESUME - Resume paused actions and reset stop state from CMD:STOP.",
    "",
    "NOTE: - can be substituted for _ in commands.",
    "There are some shortnames for keys: @C is CTRL, @S is SHIFT, @A is ALT ",
    "These @ modifiers will work in combo along wth the f-keys everything else appart from the seperator is assumed to be normal keys.",
    "In the key command you can also use \\R or \\N as RETURN, \\B as BACKSPACE, \\T as TAB.",
    ""
  };

  // Send the help message either to the Serial or to the Keyboard
  for (const char* line : helpMessage) {
    if (sendToKeyboard) {
      // Send each line of the help message to the keyboard
      currentAction.type = SEND_MESSAGE;
      currentAction.message = line;
      currentAction.index = 0;
      currentAction.newLine = true;
      // Process the action to send the line
      while (currentAction.type != NONE) {
        processCurrentAction();
      }
    } else {
      // Send each line of the help message to the Serial
      Serial.println(F(line));
    }
  }
}
