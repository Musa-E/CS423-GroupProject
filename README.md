# To-Do Application with Gesture Recognition
---
## Group Members:
- Polly Ruhnke, 
- Asher Theys, 
- Musa Elqaq, 
- Victor Lopez

---

# How To Run The Application
This section will walk you through installing and using the application on an android device ***(iOS is not currently supported)***.

## Device Specification Disclaimer
Currently, this application is built for the Google Pixel 5, running on Android 14.0 (**Version Name:** `UpsideDownCake`).  It has not been designed or developed for other versions.

---

## Step 1:
The first thing you need to do is clone the following repository into a folder of your choice.  To do this, click on the green "<> Code" dropdown button and select
"Download ZIP", as shown below:

<img width="269" alt="The Code button's dropdown menu" src="https://github.com/user-attachments/assets/685254a9-567d-4732-bb40-1c9adeff33b1">

As a side note, for users familiar with Git Bash [(<img width="14" alt="The Code button's dropdown menu" src="https://github.com/user-attachments/assets/a3006632-779e-447d-83e1-5f7adbcde4f4">)](https://git-scm.com/downloads), the repository can also be cloned using the https link *(also found in the "<> Code" button's menu)*, as shown in the above image.

---

## Step 2:
Take the downloaded ZIP file and unzip it in a folder of your choice.  Then, open the directory in Android Studio.  Once Android Studio has opened up the folder, it's recommended to
ensure that all files are present.  To do this, navigate to the right of Android Studio and click on the little elephant symbol.  This is where Gradle can download the needed files.
Click on the "Download Sources" button in the top left of this menu, as shown below:

<img width="257" alt="The Gradle Menu in Android Studio with the Download Sources button highlighted" src="https://github.com/user-attachments/assets/9debf3c1-1d4a-49d3-8ff0-fce4bb7be642">

---

## Step 3:
Once the files have been downloaded, you can proceed with one of two options.  !!!Option 1 involves using an emulator device and option 2 involves deploying the project to a connected and
compatible Android device.

### Step 3.1 (Option 1):
Navigate to the "Device Manager" menu.  Looking at the closest above image, it is the button directly below the Gradle menu.  From this menu, click on the plus sign in the top left, as shown here, and select "Create Virtual Device".  Do not worry about **Step 3.2** if you are using this option.


<img width="164" alt="Device Manager menu with the Create Virtual Device button highlighted" src="https://github.com/user-attachments/assets/760b505d-61c0-4242-8be4-5530fa175cde">


From here, scroll down in the menu until you see "Pixel 5".  Select it, and press the "Next" button as shown below:

<img width="423" alt="New Device Menu" src="https://github.com/user-attachments/assets/b028776f-bb07-4aec-aec0-58b1c2f22328">


Now you need to install the release onto the new device.  To do this, find the version called `UpsideDownCake`, for Android 14.0 as shown here:


<img width="490" alt="Device Software Version Selection Menu" src="https://github.com/user-attachments/assets/07f444a5-525f-4d7e-b70a-8252e082d718">

Lastly, you can name the device if you wish, but it won't effect the actual program.  All that's left now is to press the blue Finish button at the bottom right of the menu.

<img width="489" alt="New Virtual Device Finished Creation Menu" src="https://github.com/user-attachments/assets/43973201-068e-4b47-ba5d-187e9c757eac">

---

### Step 3.2 (Option 2): 
Use a cable with a USBC on one side to plug into the hardware Android device and use the other end to plug into your laptop. Do not worry about) **Step 3.1** if you are using this option

#### Step 3.2.1 (If the phone is not recognized by Android SDK)

These steps are taken from Android's official website (just incase you recognize them and raise an eyebrow at the similarity): [Android Website](https://developer.android.com/codelabs/basic-android-kotlin-compose-connect-device#2)

To let Android Studio communicate with your Android device, you must enable USB debugging in the Developer options settings of the device.

To show developer options and enable USB debugging:

On your Android device, tap Settings > About phone.
Tap the Build number seven times.
If prompted, enter your device password or pin. You know you succeeded when you see a You are now a developer! message. 
![image](https://github.com/user-attachments/assets/38419904-f82a-472f-8982-961fdb2da649)

Return to Settings and then tap System > Developer options.
If you don't see Developer options, tap Advanced options.
![image](https://github.com/user-attachments/assets/5c42de5c-1f81-49b0-b419-224ee6745e6b)

Tap Developer options and then tap the USB debugging toggle to turn it on.

Now, when you plug the phone into the USBC again, it will ask if you wish to debug on the hardware device. Click allow!



#### Step 3.2.2 (If the phone is recognized)

Continue onto [Step 4](## Step 4) :)

---

## Step 4:
Regardless of which option you chose, all that's left is for you to press the green play button near the top right of Android Studio.  You may need to select a project first, as shown here:

<img width="194" alt="Changing the Run Configuration and the Play Button" src="https://github.com/user-attachments/assets/e7d59019-b524-4c83-be02-ef4312a1c5a3">

Once the configuration is set, simply press on the green play button to launch the application on your device, whether it's an emulated device or a physically connected device.

---

## Step 5:
Now that you have the device running and the project has been launched on it, it's time to actually use the application.  If the application doesn't open automatically, find the 
app named "To-Do App" on the device's screen.  Select it to begin using the application.

<img width="194" alt="The application on a device's application screen" src="https://github.com/user-attachments/assets/506c205c-d244-4333-bb49-31694b28fced">

---

## Step 6:
Welcome to the main task list section of the application.  This screen is for creating new lists of tasks.  For example, you could make a list for groceries, chores, homework, trip
planning, etc.  To start, press the "+" icon in the bottom right of the screen.  Upon creating a new list, you will be prompted for a file name.  Enter in what you want the list to 
be called and press the "Ok" button.

<img width="194" alt="The main task list of the application, which is initially blank with a create list button in the bottom right of the screen." src="https://github.com/user-attachments/assets/ef8f56ca-5e1c-4bc5-854a-9ccd46066387">

**Note:** Upon creating a new list, you may be shown an alert saying `Loading error: Error while loading Part24`.  Please ignore this and press the "Ok" button. (Shown below)

<img width="185" alt="Loading Error wihle loading Part24.  Press the ok button to continue" src="https://github.com/user-attachments/assets/55ffe292-cdfd-4655-a074-d0dbcd26fd3e">

---

## Step 7:
You are now at the task-writing section of the application.  This is where you can write your own tasks and perform gestures to quickly write down whatever you want.

<img width="194" alt="The screen where a user can draw and gesture using their hand" src="https://github.com/user-attachments/assets/9cf9cd65-75b0-4500-83bd-3dd1cfd8f8c4">


To view a detailed explanation of each gesture in the application, press on the <img width="18" alt="The Gesture Help Button Icon" src="https://github.com/user-attachments/assets/6bd40b55-96cf-4423-891c-c413ef95fbe5"> button in the bottom right corner of the screen.  For simplicity, the gesture mappings are also listed below:


| Gesture | Image | Description |
| ----------- | ----------- | ----------- |
| Undo | <img width="194" alt="Draw a backwards 'C' shape to undo" src="https://github.com/user-attachments/assets/a337b730-3782-4a8c-bad9-4472453f0f16"> | Draw a backwards "C" shape |
| Redo | <img width="194" alt="Draw a 'C' shape" src="https://github.com/user-attachments/assets/2f91e26c-2554-4cec-8957-c62c929fba29"> | Draw a "C" shape |
| Convert | <img width="194" alt="Draw a line under your written text to convert text" src="https://github.com/user-attachments/assets/c9c59122-01d6-4d83-8fde-e772f4f8c2d2"> | Draw a line under your written text |
| Convert (Circle) | <img width="194" alt="Draw a circle shape around your written text to convert text" src="https://github.com/user-attachments/assets/c485db58-4343-4331-b6a5-b2f2fad312fa"> | Draw a circle shape around your written text |
| Complete | <img width="194" alt="Draw a checkmark on your converted text's status box to complete a task" src="https://github.com/user-attachments/assets/abaa7c82-c298-420d-b0f8-0be5b598f443"> | Draw a checkmark on your converted text's status box |
| Delete | <img width="194" alt="Draw an 'X' shape through your text to delete a task" src="https://github.com/user-attachments/assets/682d6c0c-8e0a-47a4-a522-2f6c56faffef"> | Draw an "X" shape through your text |
| Edit | <img width="194" alt="Draw more text after your already converted text to edit a task" src="https://github.com/user-attachments/assets/446776b7-ba00-4b4c-b859-41d6827a5193"> | Draw more text after your already converted text |

*In case of discrepancies between the in-app gesture help menu and the above table, you may need to figure out which one is accurate.  Please contact our team if you find such an issue and we will work to resolve the issue.*

---

## Step 8:
Now that you know how to use the application, try writing and gesturing whatever you want.  There may be some instances where it doesn't do as you intend, but basic functionality should
be relatively stable.  When you have finished writing what you want, you can press the back arrow in the top left corner of the screen to return to the task list screen, as shown here:

<img width="194" alt="Return to the task list view button" src="https://github.com/user-attachments/assets/f99a0f71-a015-4f25-ae9f-17a853b11ea1">

---


After using the application for a little while, you could have your task list screen filing up with all kinds of stuff (an example is shown below)!  This should allow for sufficient testing of the application's core functionality and design.

<img width="194" alt="Filled out task list screen with several example task lists present" src="https://github.com/user-attachments/assets/b08ac468-5d71-4c5d-a4ca-f69421fc4b88">

---
---

*If you or someone you know have any suggestions or requests, please contact the team about it.  While we can't guarantee that it'll be implemented, we will do our best to consider it.  Thank you!*
