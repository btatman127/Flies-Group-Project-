import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.awt.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.awt.geom.*;


public class terminalOutput {

    public static void main(String[] args) throws IOException, InterruptedException {

        JTextField startTime = new JTextField();
        JTextField endTime = new JTextField();
        JCheckBox fullLength = new JCheckBox();
        Object[] message = {
                "Please enter Start and Stop time in seconds.",
                "Start time:", startTime,
                "End Time:", endTime,
                "Select full video:", fullLength
        };

       int option = JOptionPane.showConfirmDialog(null, message, "Time Select", JOptionPane.OK_CANCEL_OPTION);
    }
}
