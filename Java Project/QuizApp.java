import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.json.JSONArray;
import org.json.JSONObject;

public class QuizApp extends JFrame implements ActionListener {

    // --- UI Components for configuration screen ---
    private JPanel configPanel;
    private JComboBox<String> catBox;
    private JComboBox<String> diffBox;
    private JComboBox<String> queCntBox;
    private JButton startBtn;

    // --- UI Components for quiz screen ---
    private JPanel quizPanel;
    private JLabel queLabel;
    private JRadioButton[] options;
    private JButton nxtBtn, prevBtn;
    private ButtonGroup optionGroup;
    private JLabel timerLabel;
    
    // --- UI Components for analysis screen ---
    private JPanel analysisPanel;
    
    // Timer variables
    private Timer timeCnt;
    private int timeLeft; // in seconds

    // Data for quiz
    private List<Question> quizQue = new ArrayList<>();
    private int currQueIdx = 0;
    private List<Integer> userAns = new ArrayList<>();

    // Constants for quiz timing (15 seconds per question)
    private final int SECONDS_PER_QUESTION = 30;

    public QuizApp() {
        setTitle("Quiz Application");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new CardLayout());

        buildConfigScreen();
        buildQuizScreen();
        buildAnalysisScreen();

        setVisible(true);
    }

    // ------------------ Question class for data ------------------
    class Question {
        String que;
        String[] options;
        int ans; // index 0-3
        String category;
        String difficulty;

        public Question(String que, String[] options, int ans, String category, String difficulty) {
            this.que = que;
            this.options = options;
            this.ans = ans;
            this.category = category;
            this.difficulty = difficulty;
        }
    }

    //---------------------------- Build configuration panel ----------------------------
    private void buildConfigScreen() {
        configPanel = new JPanel();
        configPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Font to be used on the config screen (size 15)
        Font configFont = new Font("SansSerif", Font.PLAIN, 15);
        
        // Category selection
        JLabel categoryLabel = new JLabel("Select Category:");
        categoryLabel.setFont(configFont);
        gbc.gridx = 0;
        gbc.gridy = 0;
        configPanel.add(categoryLabel, gbc);
        
        String[] categories = {"Science", "History", "Geography", "Literature", "Sports", "Entertainment", "General Knowledge"};
        catBox = new JComboBox<>(categories);
        catBox.setFont(configFont);
        gbc.gridx = 1;
        configPanel.add(catBox, gbc);
        
        // Difficulty selection
        JLabel difficultyLabel = new JLabel("Select Difficulty:");
        difficultyLabel.setFont(configFont);
        gbc.gridx = 0;
        gbc.gridy = 1;
        configPanel.add(difficultyLabel, gbc);
        
        String[] difficulties = {"Easy", "Medium", "Hard", "Mixed"};
        diffBox = new JComboBox<>(difficulties);
        diffBox.setFont(configFont);
        gbc.gridx = 1;
        configPanel.add(diffBox, gbc);
        
        // Number of questions selection
        JLabel questionCountLabel = new JLabel("Select Number of Questions:");
        questionCountLabel.setFont(configFont);
        gbc.gridx = 0;
        gbc.gridy = 2;
        configPanel.add(questionCountLabel, gbc);
        
        String[] questionCounts = {"10", "15", "30"};
        queCntBox = new JComboBox<>(questionCounts);
        queCntBox.setFont(configFont);
        gbc.gridx = 1;
        configPanel.add(queCntBox, gbc);
        
        // Start Quiz button
        startBtn = new JButton("Start Quiz");
        startBtn.setFont(configFont);
        startBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    startQuiz();
                } catch (Exception ex) {
                    // In case of any exception, show an error message.
                    JOptionPane.showMessageDialog(QuizApp.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        configPanel.add(startBtn, gbc);
        
        add(configPanel, "ConfigPanel");
    }

    //------------------- Build quiz panel UI components ----------------------
    private void buildQuizScreen() {
        quizPanel = new JPanel(new BorderLayout());
        
        // Top panel: question text and timer
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        queLabel = new JLabel("Question will appear here");
        queLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        topPanel.add(queLabel, BorderLayout.CENTER);
        
        timerLabel = new JLabel("Time: 00:00");
        timerLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        topPanel.add(timerLabel, BorderLayout.EAST);
        quizPanel.add(topPanel, BorderLayout.NORTH);
        
        // Center panel: options
        JPanel optionsPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        optionGroup = new ButtonGroup();
        options = new JRadioButton[4];
        for (int i = 0; i < 4; i++) {
            options[i] = new JRadioButton("Option " + (i + 1));
            options[i].setFont(new Font("SansSerif", Font.PLAIN, 16));
            optionGroup.add(options[i]);
            optionsPanel.add(options[i]);
        }
        
        quizPanel.add(optionsPanel, BorderLayout.CENTER);
        
        // Bottom panel: navigation buttons
        JPanel buttonPanel = new JPanel();
        prevBtn = new JButton("Previous");
        prevBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        prevBtn.addActionListener(this);
        prevBtn.setEnabled(false);
        buttonPanel.add(prevBtn);
        
        nxtBtn = new JButton("Next");
        nxtBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        nxtBtn.addActionListener(this);
        buttonPanel.add(nxtBtn);
        quizPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(quizPanel, "QuizPanel");
    }
    
    // -------------- Build analysis panel --------------
    private void buildAnalysisScreen() {
        analysisPanel = new JPanel();
        analysisPanel.setLayout(new BorderLayout());
        // Add a scroll pane to handle many questions
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        analysisPanel.add(scrollPane, BorderLayout.CENTER);
        add(analysisPanel, "AnalysisPanel");
    }
    
    // ---------------------------------------------------
    // Update the analysis panel with detailed results for each question
    private void updateAnalysisScreen() {
        // Main panel to hold all question analysis entries
        JPanel analysisContent = new JPanel();
        analysisContent.setLayout(new BoxLayout(analysisContent, BoxLayout.Y_AXIS));
        analysisContent.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        for (int i = 0; i < quizQue.size(); i++) {
            Question q = quizQue.get(i);
            int userAnsIndex = userAns.get(i);
            
            // Create a panel for each question analysis
            JPanel qPanel = new JPanel();
            qPanel.setLayout(new BoxLayout(qPanel, BoxLayout.Y_AXIS));
            qPanel.setBorder(BorderFactory.createTitledBorder("Question " + (i + 1)));
            
            // Display the question text
            JLabel qLabel = new JLabel(q.que);
            qLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            qPanel.add(qLabel);
            
            // Display the user's answer
            String userAnswerText = (userAnsIndex == -1) ? "No answer selected" : q.options[userAnsIndex];
            JLabel userLabel = new JLabel("Your Answer: " + userAnswerText);
            userLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            qPanel.add(userLabel);
            
            // Display the correct answer
            JLabel correctLabel = new JLabel("Correct Answer: " + q.options[q.ans]);
            correctLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            qPanel.add(correctLabel);
            
            // Indicate if the answer was correct, not answered, or incorrect
            String resultText;
            if (userAnsIndex == -1) {
                resultText = "Not Answered";
            } else {
                resultText = (userAnsIndex == q.ans) ? "Correct" : "Incorrect";
            }
            JLabel resultLabel = new JLabel("Result: " + resultText);
            resultLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
            qPanel.add(resultLabel);
            
            // Add some spacing between questions
            qPanel.add(Box.createVerticalStrut(10));
            analysisContent.add(qPanel);
        }
        
        // Add an Exit button at the bottom
        JButton exitButton = new JButton("Exit");
        exitButton.setAlignmentX(CENTER_ALIGNMENT);
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        analysisContent.add(exitButton);
        
        // Update the scroll pane's viewport with the analysis content
        JScrollPane scrollPane = (JScrollPane) analysisPanel.getComponent(0);
        scrollPane.setViewportView(analysisContent);
    }

    // ---------------------------------------------------
    // Maps our category names to Open Trivia DB category IDs
    private int getCategoryId(String categoryName) {
        switch (categoryName) {
            case "Science": return 17; // Science & Nature
            case "History": return 23; // History
            case "Geography": return 22; // Geography
            case "Literature": return 10; // Entertainment: Books
            case "Sports": return 21; // Sports
            case "Entertainment": return 11; // Entertainment: Film
            case "General Knowledge": return 9;  // General Knowledge
            default: return 9;
        }
    }

    // ---------------------------------------------------
    // Fetch questions from Open Trivia DB API using user selections
    // (Note: This method now uses a try–catch block at the error check)
    private List<Question> fetchQue(String category, String difficulty, int numQuestions) {
        List<Question> questionsList = new ArrayList<>();
        try {
            int catId = getCategoryId(category);
            String baseUrl = "https://opentdb.com/api.php";
            // Build query parameters
            StringBuilder urlStr = new StringBuilder(baseUrl);
            urlStr.append("?amount=").append(numQuestions);
            urlStr.append("&category=").append(catId);
            if (!difficulty.equalsIgnoreCase("Mixed")) {
                urlStr.append("&difficulty=").append(URLEncoder.encode(difficulty.toLowerCase(), "UTF-8"));
            }
            urlStr.append("&type=multiple");
            URL url = new URL(urlStr.toString());
                
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
                
            // Read API response
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder responseStr = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                responseStr.append(inputLine);
            }
            in.close();
                
            JSONObject jsonResponse = new JSONObject(responseStr.toString());
            int responseCode = jsonResponse.getInt("response_code");
            if (responseCode != 0) {
                // Use try–catch block to throw an error at this point (line ~152)
                try {
                    throw new Exception("API did not return valid results.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return questionsList;
                }
            }
                
            JSONArray results = jsonResponse.getJSONArray("results");
            for (int i = 0; i < results.length(); i++) {
                JSONObject obj = results.getJSONObject(i);
                String que = obj.getString("question");
                String ans = obj.getString("correct_answer");
                JSONArray incorrectArray = obj.getJSONArray("incorrect_answers");
                List<String> opts = new ArrayList<>();
                opts.add(ans);
                for (int j = 0; j < incorrectArray.length(); j++) {
                    opts.add(incorrectArray.getString(j));
                }
                // Randomize options
                Collections.shuffle(opts);
                String[] optionsArr = opts.toArray(new String[0]);
                // Find index of correct answer after shuffle
                int correctIndex = -1;
                for (int j = 0; j < optionsArr.length; j++) {
                    if (optionsArr[j].equals(ans)) {
                        correctIndex = j;
                        break;
                    }
                }
                // Create a new Question object using the selected category and difficulty from input
                Question q = new Question(que, optionsArr, correctIndex, category, difficulty);
                questionsList.add(q);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching questions from API", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return questionsList;
    }

    // ---------------------------------------------------
    // Start the quiz after configuration is selected.
    // This method now uses try–catch to handle exceptions.
    private void startQuiz() throws Exception {
        String selectedCategory = (String) catBox.getSelectedItem();
        String selectedDifficulty = (String) diffBox.getSelectedItem();
        int numQuestions = Integer.parseInt((String) queCntBox.getSelectedItem());
        
        // Fetch questions from API using the user's selections
        quizQue = fetchQue(selectedCategory, selectedDifficulty, numQuestions);
        if (quizQue.size() < numQuestions) {
            throw new Exception("Not enough questions available from API.");
        }
        
        // Initialize user answers list
        userAns.clear();
        for (int i = 0; i < quizQue.size(); i++) {
            userAns.add(-1);
        }
        
        // Set timer based on number of questions (15 seconds per question)
        timeLeft = numQuestions * SECONDS_PER_QUESTION;
        
        // Reset index and update quiz UI with first question
        currQueIdx = 0;
        updateQuestion();
        
        // Switch panels using CardLayout
        CardLayout cl = (CardLayout) getContentPane().getLayout();
        cl.show(getContentPane(), "QuizPanel");
        
        // Start timer
        if (timeCnt != null && timeCnt.isRunning()) {
            timeCnt.stop();
        }
        timeCnt = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                timeLeft--;
                updateTimerLabel();
                if (timeLeft <= 0) {
                    timeCnt.stop();
                    JOptionPane.showMessageDialog(QuizApp.this, "Time's up! Submitting your quiz.");
                    showResult();
                }
            }
        });
        timeCnt.start();
    }

    // ---------------------------------------------------
    // Update timer label display
    private void updateTimerLabel() {
        int minutes = timeLeft / 60;
        int seconds = timeLeft % 60;
        timerLabel.setText(String.format("Time: %02d:%02d", minutes, seconds));
    }

    // ---------------------------------------------------
    // Update the quiz question and options based on currQueIdx
    private void updateQuestion() {
        Question currentQuestion = quizQue.get(currQueIdx);
        // Updated question label to include the question number
        queLabel.setText("<html>Question " + (currQueIdx + 1) + " of " + quizQue.size() 
                                + ":<br>" + currentQuestion.que + "</html>");
        for (int i = 0; i < 4; i++) {
            options[i].setText(currentQuestion.options[i]);
        }
        loadUserAnswer();
        prevBtn.setEnabled(currQueIdx > 0);
        updateNavigationButton();
    }

    // ---------------------------------------------------
    // Save the current selection into the userAns list
    private void saveUserAnswer() {
        for (int i = 0; i < 4; i++) {
            if (options[i].isSelected()) {
                userAns.set(currQueIdx, i);
                return;
            }
        }
        userAns.set(currQueIdx, -1);
    }

    // ---------------------------------------------------
    // Loads saved answer for the current question into the UI
    private void loadUserAnswer() {
        optionGroup.clearSelection();
        int savedAnswer = userAns.get(currQueIdx);
        if (savedAnswer != -1 && savedAnswer < options.length) {
            options[savedAnswer].setSelected(true);
        }
    }

    // ---------------------------------------------------
    // Change the next button label to "Submit" on the last question
    private void updateNavigationButton() {
        if (currQueIdx == quizQue.size() - 1) {
            nxtBtn.setText("Submit");
        } else {
            nxtBtn.setText("Next");
        }
    }

    // ---------------------------------------------------
    // Calculate and display the final score and show the analysis screen
    private void showResult() {
        if (timeCnt.isRunning()) {
            timeCnt.stop();
        }
        int score = 0;
        for (int i = 0; i < quizQue.size(); i++) {
            if (userAns.get(i) == quizQue.get(i).ans) {
                score++;
            }
        }
        JOptionPane.showMessageDialog(this, "Your score: " + score + " out of " + quizQue.size());
        updateAnalysisScreen();
        CardLayout cl = (CardLayout) getContentPane().getLayout();
        cl.show(getContentPane(), "AnalysisPanel");
    }

    // ---------------------------------------------------
    // Navigation button actions
    @Override
    public void actionPerformed(ActionEvent e) {
        saveUserAnswer();
        if (e.getSource() == nxtBtn) {
            if (currQueIdx == quizQue.size() - 1) {
                showResult();
            } else {
                currQueIdx++;
                updateQuestion();
            }
        } else if (e.getSource() == prevBtn) {
            if (currQueIdx > 0) {
                currQueIdx--;
                updateQuestion();
            }
        }
    }
    // ---------------------------------------------------
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(() -> {
            try {
                new QuizApp();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}