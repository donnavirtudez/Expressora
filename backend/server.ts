import express, { Request, Response } from "express";
import bodyParser from "body-parser";
import cors from "cors";
import dotenv from "dotenv";
import nodemailer from "nodemailer";
import { OAuth2Client } from "google-auth-library";
import { GoogleGenerativeAI } from "@google/generative-ai";
import admin from "firebase-admin";
import crypto from "crypto";
import path from "path";
import https from "https";

dotenv.config();
const app = express();
const PORT: number = Number(process.env.PORT) || 3000;

app.use(cors());
app.use(bodyParser.json());

if (!admin.apps.length) {
  console.log("🔥 Initializing Firebase Admin SDK...");
  admin.initializeApp({
    credential: admin.credential.cert(
      require(path.join(__dirname, "./serviceAccountKey.json"))
    ),
  });
}
const db = admin.firestore();
console.log("✅ Firestore initialized successfully");

const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: process.env.EMAIL_USER,
    pass: process.env.EMAIL_PASS,
  },
});

const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

// Initialize Gemini AI
let genAI: GoogleGenerativeAI | null = null;
const geminiApiKey = process.env.GEMINI_API_KEY?.trim();

if (geminiApiKey) {
  try {
    genAI = new GoogleGenerativeAI(geminiApiKey);
    console.log("✅ Gemini AI initialized successfully");
    console.log(`🔑 API Key: ${geminiApiKey.substring(0, 10)}...${geminiApiKey.substring(geminiApiKey.length - 4)}`);
  } catch (error: any) {
    console.error("❌ Failed to initialize Gemini AI:", error.message);
    genAI = null;
  }
} else {
  console.warn("⚠️  Gemini API key not found. AI quiz generation will be disabled.");
  console.warn("💡 Add GEMINI_API_KEY to your .env file to enable AI features.");
}

interface OtpEntry {
  code: string;
  createdAt: number;
  purpose: "register" | "reset";
}
const otpStore: { [email: string]: OtpEntry } = {};

setInterval(() => {
  const now = Date.now();
  const expiryMs = 3 * 60 * 1000;
  let removed = 0;
  for (const email in otpStore) {
    if (now - otpStore[email].createdAt > expiryMs) {
      delete otpStore[email];
      removed++;
    }
  }
  if (removed > 0) console.log(`🧹 Cleaned ${removed} expired OTP(s)`);
}, 60 * 1000);

app.get("/", (req, res) => {
  console.log("📡 GET / - Backend online");
  return res.send("✅ Expressora Backend Running");
});

app.post("/reg-send-otp", async (req: Request, res: Response) => {
  const { email } = req.body;
  if (!email)
    return res.status(400).json({ success: false, message: "Email is required" });

  try {
    const existingUser = await db.collection("users").where("email", "==", email).get();
    if (!existingUser.empty) {
      return res.status(409).json({
        success: false,
        message: "Email already registered. Please log in instead.",
      });
    }

    const otp = Math.floor(10000 + Math.random() * 90000).toString();
    otpStore[email] = { code: otp, createdAt: Date.now(), purpose: "register" };
    console.log(`🔐 [REG] OTP for ${email}: ${otp}`);

    await transporter.sendMail({
        from: `"Expressora" <${process.env.EMAIL_USER}>`,
        to: email,
        subject: "Expressora | Email Verification Code",
        html: `
       <div style="font-family:'Inter',Arial,sans-serif; background-color:#f8fafc; padding:48px 24px; text-align:center;">
         <div style="max-width:720px; margin:auto; background:white; border-radius:20px; padding:44px 40px; box-shadow:0 8px 24px rgba(0,0,0,0.06); border:1px solid #f1f1f1;">
           <img src="https://res.cloudinary.com/dugthtx3b/image/upload/v1761753085/expressora_logo_emnorx.png"
                alt="Expressora Logo"
                width="80"
                style="margin-bottom:20px;">
           <h2 style="color:#111827; font-weight:700; font-size:22px; margin-bottom:8px;">Verify Your Email</h2>
           <p style="color:#374151; font-size:15px; line-height:1.6; margin-bottom:20px;">
             Welcome to <strong>Expressora</strong>!<br>
             Please use the verification code below to confirm your email address.
           </p>

           <div style="display:inline-block; background:linear-gradient(135deg, #FACC15, #FFD84D); color:#111; font-weight:700; font-size:24px; padding:12px 28px; border-radius:10px; letter-spacing:3px; margin:8px 0 10px; box-shadow:0 3px 8px rgba(250,204,21,0.25);">
             ${otp}
           </div>

           <p style="color:#6b7280; font-size:14px; margin-top:8px;">
             This code will expire in <strong>3 minutes</strong>.
           </p>

           <hr style="margin:32px 0; border:none; border-top:1px solid #e5e7eb;">

           <p style="color:#9ca3af; font-size:13px; line-height:1.5;">
             Didn’t request this verification?<br>
             You can safely ignore this email.
           </p>

           <p style="color:#bdbdbd; font-size:12px; margin-top:24px;">
             &copy; ${new Date().getFullYear()} Expressora. All rights reserved.
           </p>
         </div>
       </div>
    `,
      });

    return res.json({ success: true, message: "OTP sent successfully" });
  } catch (error) {
    console.error("❌ /reg-send-otp error:", error);
    return res.status(500).json({ success: false, message: "Failed to send OTP" });
  }
});

app.post("/reg-verify-otp", (req: Request, res: Response) => {
  const { email, otp } = req.body;
  if (!email || !otp)
    return res.status(400).json({ success: false, message: "Email and OTP required" });

  const entry = otpStore[email];
  if (!entry || entry.purpose !== "register")
    return res.status(400).json({ success: false, message: "OTP not found or expired" });

  if (Date.now() - entry.createdAt > 3 * 60 * 1000) {
    delete otpStore[email];
    return res.status(400).json({ success: false, message: "OTP expired" });
  }

  if (entry.code === otp) {
    delete otpStore[email];
    console.log(`✅ [REG] OTP verified for ${email}`);
    return res.json({ success: true, message: "OTP verified successfully" });
  } else {
    return res.status(400).json({ success: false, message: "Invalid OTP" });
  }
});

app.post("/reset-send-otp", async (req: Request, res: Response) => {
  const { email } = req.body;
  if (!email)
    return res.status(400).json({ success: false, message: "Email is required" });

  try {
    const userSnap = await db.collection("users").where("email", "==", email).get();
    if (userSnap.empty) {
      return res.status(404).json({
        success: false,
        message: "Email not found. Please register first.",
      });
    }

    const otp = Math.floor(10000 + Math.random() * 90000).toString();
    otpStore[email] = { code: otp, createdAt: Date.now(), purpose: "reset" };
    console.log(`🔐 [RESET] OTP for ${email}: ${otp}`);

    await transporter.sendMail({
        from: `"Expressora" <${process.env.EMAIL_USER}>`,
        to: email,
        subject: "Expressora | Password Reset Code",
        html: `
        <div style="font-family:'Inter',Arial,sans-serif; background-color:#f8fafc; padding:48px 24px; text-align:center;">
          <div style="max-width:720px; margin:auto; background:white; border-radius:20px; padding:44px 40px; box-shadow:0 8px 24px rgba(0,0,0,0.06); border:1px solid #f1f1f1;">
            <img src="https://res.cloudinary.com/dugthtx3b/image/upload/v1761753085/expressora_logo_emnorx.png"
                 alt="Expressora Logo"
                 width="80"
                 style="margin-bottom:20px;">
            <h2 style="color:#111827; font-weight:700; font-size:22px; margin-bottom:8px;">Reset Your Password</h2>
            <p style="color:#374151; font-size:15px; line-height:1.6; margin-bottom:20px;">
              We received a request to reset your <strong>Expressora</strong> account password.<br>
              Please use the verification code below to continue.
            </p>

            <div style="display:inline-block; background:linear-gradient(135deg, #FACC15, #FFD84D); color:#111; font-weight:700; font-size:24px; padding:12px 28px; border-radius:10px; letter-spacing:3px; margin:8px 0 10px; box-shadow:0 3px 8px rgba(250,204,21,0.25);">
              ${otp}
            </div>

            <p style="color:#6b7280; font-size:14px; margin-top:8px;">
              This code will expire in <strong>3 minutes</strong>.
            </p>

            <hr style="margin:32px 0; border:none; border-top:1px solid #e5e7eb;">

            <p style="color:#9ca3af; font-size:13px; line-height:1.5;">
              Didn’t request a password reset?<br>
              You can safely ignore this email.
            </p>

            <p style="color:#bdbdbd; font-size:12px; margin-top:24px;">
              &copy; ${new Date().getFullYear()} Expressora. All rights reserved.
            </p>
          </div>
        </div>
        `,
      });

    return res.json({ success: true, message: "Reset OTP sent successfully" });
  } catch (error) {
    console.error("❌ /reset-send-otp error:", error);
    return res.status(500).json({ success: false, message: "Failed to send reset OTP" });
  }
});

app.post("/reset-verify-otp", (req: Request, res: Response) => {
  const { email, otp } = req.body;
  if (!email || !otp)
    return res.status(400).json({ success: false, message: "Email and OTP required" });

  const entry = otpStore[email];
  if (!entry || entry.purpose !== "reset")
    return res.status(400).json({ success: false, message: "OTP not found or expired" });

  if (Date.now() - entry.createdAt > 3 * 60 * 1000) {
    delete otpStore[email];
    return res.status(400).json({ success: false, message: "OTP expired" });
  }

  if (entry.code === otp) {
    console.log(`✅ [RESET] OTP verified for ${email}`);
    return res.json({ success: true, message: "OTP verified successfully" });
  } else {
    console.log(`❌ [RESET] Invalid OTP for ${email}`);
    return res.status(400).json({ success: false, message: "Invalid OTP" });
  }
});

app.post("/reset-password", async (req: Request, res: Response) => {
  const { email, newPassword } = req.body;
  if (!email || !newPassword)
    return res.status(400).json({ success: false, message: "Email and password required" });

  try {
    const userSnap = await db.collection("users").where("email", "==", email).get();
    if (userSnap.empty)
      return res.status(404).json({ success: false, message: "User not found" });

    const docId = userSnap.docs[0].id;
    await db.collection("users").doc(docId).update({ 
      password: newPassword,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    console.log(`🔑 Password reset for ${email}`);
    return res.json({ success: true, message: "Password reset successful" });
  } catch (error) {
    console.error("❌ /reset-password error:", error);
    return res.status(500).json({ success: false, message: "Failed to reset password" });
  }
});

app.post("/google-auth", async (req: Request, res: Response) => {
  const { token } = req.body;
  if (!token)
    return res.status(400).json({ success: false, message: "Token required" });

  try {
    const ticket = await googleClient.verifyIdToken({
      idToken: token,
      audience: process.env.GOOGLE_CLIENT_ID,
    });
    const payload = ticket.getPayload();
    if (!payload) throw new Error("Invalid Google token");

    const { email, name, picture } = payload;
    console.log(`✅ Google verified user: ${email}`);
    return res.status(200).json({ success: true, user: { email, name, picture } });
  } catch (error: any) {
    console.error("❌ Google auth failed:", error.message);
    return res.status(401).json({ success: false, message: error.message });
  }
});

// AI-powered quiz question generation endpoint
app.post("/generate-quiz-questions", async (req: Request, res: Response) => {
  const { difficulty, count = 5 } = req.body;

  if (!difficulty) {
    return res.status(400).json({ 
      success: false, 
      message: "Difficulty level is required" 
    });
  }

  if (!genAI) {
    return res.status(503).json({ 
      success: false, 
      message: "AI service is not available. Please set GEMINI_API_KEY in .env file and restart the server." 
    });
  }

  try {
    // Get available model (tries different models until one works)
    const { model, modelName } = await getAvailableModel();
    console.log(`🤖 Using model: ${modelName} for quiz generation`);

    const difficultyDescriptions: { [key: string]: string } = {
      EASY: "Beginner level - Simple, common sign language vocabulary and basic phrases. Questions should be straightforward with clear visual signs. Use simple one-word answers like: Yes, No, Love, Home, Good, Bad, Food, Water, Hello, Thanks, Please, Sorry, Happy, Angry, ASL, FSL, Morning, Evening, Night, Today, Tomorrow, Yesterday (all 10 chars or less).",
      MEDIUM: "Intermediate level - More complex signs, phrases, and common expressions. Questions require understanding of context. Use one-word answers like: Family, Friend, School, Work, Help, Stop, Wait, Come, Go, Stay, Sleep, Eat, Drink, Think, Feel, Know, See, Hear, Want, Need, Person, People, Child, Woman, Man, Woman, Brother, Sister, Mother, Father (all 10 chars or less).",
      DIFFICULT: "Advanced level - Complex signs, idioms, and nuanced expressions. Questions require deeper understanding and interpretation. Use one-word answers like: Learn, Teach, Study, Write, Read, Speak, Sign, Quiet, Loud, Fast, Slow, Easy, Hard, Right, Wrong, True, False, Here, There, Where, When, What, Who, Why, How, Understand, Remember, Forget, Believe, Accept, Reject (all 10 chars or less).",
      PRO: "Expert level - Master-level signs, technical vocabulary, and sophisticated expressions. Questions are challenging and require expertise. Use one-word answers like: Expert, Master, Skill, Talent, Gift, Power, Force, Energy, Light, Dark, Bright, Clear, Sharp, Smart, Wise, Brave, Calm, Peace, Hope, Faith, Trust, Honor, Pride, Dream, Goal, Wisdom, Courage, Respect, Dignity, Freedom (all 10 chars or less)."
    };

    const difficultyDesc = difficultyDescriptions[difficulty.toUpperCase()] || difficultyDescriptions.EASY;
    const numQuestions = Math.min(Math.max(parseInt(count) || 5, 1), 10); // Between 1-10 questions

    const prompt = `You are an AI assistant helping to create quiz questions for Expressora: An AI-Powered Mobile Application for American and Filipino Sign Language.

Generate exactly ${numQuestions} multiple-choice quiz questions for ${difficulty} difficulty level.

IMPORTANT: These questions are for a quiz where users will see SIGN LANGUAGE IMAGES (pictures of hand signs) and need to identify what the sign means or what sign language it is.

Guidelines:
- ${difficultyDesc}
- Each question should be about IDENTIFYING SIGN LANGUAGE from images
- Questions should test knowledge of sign vocabulary meanings, sign identification, or differences between ASL and FSL signs
- Questions should be phrased as if the user is LOOKING AT A SIGN LANGUAGE IMAGE and needs to identify it
- Question format examples:
  * "What does this sign mean? (showing a sign image)"
  * "What sign language is this sign from? (ASL or FSL)"
  * "Which word/phrase does this sign represent?"
  * "What is the meaning of this ASL/FSL sign?"
- Questions can cover: sign vocabulary (words/phrases like "thank you", "hello", "goodbye", "yes", "no", "please", "sorry", "love", "family", etc.), sign meanings, common ASL/FSL expressions, basic sign language concepts, differences between ASL and FSL signs
- Focus on common, everyday signs that users would recognize and learn
- Format: Each question should have a question text, one correct answer, and 3 wrong options (distractors)
- Keep question text under 500 characters
- ABSOLUTELY CRITICAL FOR ALL DIFFICULTY LEVELS (EASY, MEDIUM, DIFFICULT, PRO): Answer choices MUST be EXACTLY ONE WORD ONLY with MAXIMUM 10 CHARACTERS - NO EXCEPTIONS, NO SPACES, NO HYPHENS, NO MULTIPLE WORDS. The design uses boxes (50% screen width, 56dp height) in a 2-column grid layout. If text exceeds 10 characters, users CANNOT READ IT because there is NO ellipsis or scrolling - the text will be CUT OFF and unreadable.
- THIS RULE APPLIES TO ALL DIFFICULTY LEVELS: Even for PRO difficulty with complex signs, you MUST use one-word answers that are 10 characters or less. For example: "Thank you" → "Thanks" (6), "Goodbye" → "Bye" (3), "I love you" → "Love" (4), "Good morning" → "Morning" (7) or "Hello" (5)
- STRICT EXAMPLES by difficulty level (all 10 chars or less):
  * EASY: "Yes" (3), "No" (2), "Love" (4), "Home" (4), "Good" (4), "Bad" (3), "Food" (4), "Water" (5), "Hello" (5), "Thanks" (6), "Please" (6), "Sorry" (5), "Happy" (5), "Morning" (7), "Evening" (7), "Tomorrow" (8), "Yesterday" (9), "ASL" (3), "FSL" (3)
  * MEDIUM: "Family" (6), "Friend" (6), "School" (6), "Work" (4), "Help" (4), "Stop" (4), "Wait" (4), "Person" (6), "People" (6), "Brother" (7), "Sister" (6), "Mother" (6), "Father" (6), "Child" (5), "Woman" (5), "Man" (3)
  * DIFFICULT: "Learn" (5), "Teach" (5), "Study" (5), "Write" (5), "Read" (4), "Speak" (5), "Sign" (4), "Quiet" (5), "Understand" (10), "Remember" (8), "Forget" (6), "Believe" (7), "Accept" (6), "Reject" (6)
  * PRO: "Expert" (6), "Master" (6), "Skill" (5), "Talent" (6), "Power" (5), "Energy" (6), "Wisdom" (6), "Courage" (7), "Respect" (7), "Dignity" (7), "Freedom" (7), "Liberty" (7)
- NEVER use phrases or multi-word answers. If a sign represents multiple words, choose the MOST IMPORTANT SINGLE WORD. 
- IMPORTANT: Use the ORIGINAL word if it fits within 10 characters. For example: "Morning" (7 chars) → keep "Morning", "Understand" (10 chars) → keep "Understand", "Tomorrow" (8 chars) → keep "Tomorrow". Only use shortcuts/alternatives if the word exceeds 10 characters.
- Examples of when to shortcut: "Goodbye" (7 chars but common to use "Bye" 3 chars), "Thank you" (phrase) → "Thanks" (6 chars). But "Morning" (7 chars) should stay as "Morning", not "Hello".
- Before returning JSON, manually count EVERY character in EVERY choice to ensure it's exactly 10 or fewer characters.
- IF YOU CANNOT FIND A ONE-WORD ANSWER THAT IS 10 CHARACTERS OR LESS, choose a different sign/question that allows for a shorter answer.
- Wrong options should be plausible but clearly incorrect
- Wrong options should not be duplicates of each other or the correct answer
- Make questions relevant to the Expressora app context - a mobile application for learning both ASL and FSL through visual sign identification

Return ONLY a valid JSON array with this exact structure:
[
  {
    "question": "Question text here (phrased as if user is looking at a sign image)",
    "correctAnswer": "Hello",
    "wrongOptions": ["Thanks", "Yes", "Love"]
  },
  ...
]

FINAL CRITICAL REMINDER: ALL answer choices (correctAnswer and wrongOptions) for ALL difficulty levels (EASY, MEDIUM, DIFFICULT, PRO) MUST be EXACTLY ONE WORD ONLY with MAXIMUM 10 CHARACTERS. NO PHRASES, NO MULTIPLE WORDS, NO SPACES, NO HYPHENS. Users CANNOT read choices that exceed 10 characters - there is no ellipsis, no scrolling, text will be cut off. 

Examples of required transformations:
- "Thank you" → "Thanks" (6 chars)
- "Goodbye" → "Bye" (3 chars)  
- "I love you" → "Love" (4 chars)
- "Good morning" → "Morning" (7 chars) - NOW ALLOWED with 10 char limit
- "See you later" → "Later" (5 chars) or "Bye" (3 chars)
- "How are you" → "How" (3 chars) or "Fine" (4 chars)
- "I understand" → "Understand" (10 chars) - NOW ALLOWED

Before returning JSON, count every character in every choice. If any choice is longer than 10 characters or contains spaces, you MUST replace it with a shorter one-word alternative. This applies to EASY, MEDIUM, DIFFICULT, and PRO difficulty levels equally.

Make sure the JSON is valid and can be parsed. Do not include any explanation or markdown formatting, just the JSON array.`;

    console.log(`🤖 Generating ${numQuestions} quiz questions for ${difficulty} difficulty...`);

    const result = await model.generateContent(prompt);
    const response = await result.response;
    const text = response.text();

    // Extract JSON from response (remove markdown code blocks if present)
    let jsonText = text.trim();
    if (jsonText.startsWith("```json")) {
      jsonText = jsonText.replace(/```json\n?/g, "").replace(/```\n?/g, "");
    } else if (jsonText.startsWith("```")) {
      jsonText = jsonText.replace(/```\n?/g, "");
    }

    // Parse the JSON
    const questions = JSON.parse(jsonText);

    if (!Array.isArray(questions) || questions.length === 0) {
      throw new Error("Invalid response format from AI");
    }

    // Helper function to validate and fix choices (one word, max 10 characters)
    // IMPORTANT: Preserve original word if it fits (10 chars or less), only shortcut if it exceeds 10
    const validateAndFixChoice = (choice: string, fallbackOptions: string[] = ["Yes", "No", "Love", "Home", "Good"]): string => {
      if (!choice) return fallbackOptions[0]; // Default fallback
      
      let cleaned = choice.trim();
      
      // Remove any spaces, hyphens, and take only the first word
      const firstWord = cleaned.split(/[\s\-]+/)[0];
      
      // If the word is 10 characters or less and is a single word, keep it as is
      if (firstWord.length <= 10 && !firstWord.includes(" ")) {
        return firstWord;
      }
      
      // Only if it exceeds 10 characters, try to find a shorter alternative
      if (firstWord.length > 10) {
        // Try to find a shorter synonym or use fallback
        // Only apply shortcuts for words that are too long
        const alternatives: { [key: string]: string } = {
          "Goodbye": "Bye",
          "Welcome": "Hello",
          "Afternoon": "Noon",
        };
        
        const lowerFirst = firstWord.toLowerCase();
        if (alternatives[firstWord]) {
          cleaned = alternatives[firstWord];
        } else if (lowerFirst.startsWith("goodbye")) {
          cleaned = "Bye";
        } else {
          // Last resort: truncate only if no alternative found
          cleaned = firstWord.substring(0, 10);
          console.warn(`⚠️ Choice "${choice}" was too long (${firstWord.length} chars) and truncated to "${cleaned}". AI should have generated a shorter word.`);
        }
      } else {
        // If it's 10 or less but has spaces, just take first word
        cleaned = firstWord;
      }
      
      // Ensure it's not empty
      if (cleaned.length === 0) {
        cleaned = fallbackOptions[0];
      }
      
      // Final check - should never exceed 10, but double-check
      if (cleaned.length > 10) {
        cleaned = cleaned.substring(0, 10);
        console.warn(`⚠️ Choice "${choice}" still exceeded 10 chars after processing. Final: "${cleaned}"`);
      }
      
      return cleaned;
    };

    // Validate each question has required fields and fix choices
    const validQuestions = questions
      .filter((q: any) =>
        q.question &&
        q.correctAnswer &&
        Array.isArray(q.wrongOptions) &&
        q.wrongOptions.length >= 1 &&
        q.wrongOptions.length <= 3
      )
      .map((q: any) => ({
        question: q.question.trim(),
        correctAnswer: validateAndFixChoice(q.correctAnswer),
        wrongOptions: q.wrongOptions
          .map((opt: string) => validateAndFixChoice(opt))
          .slice(0, 3)
          .filter((opt: string, index: number, arr: string[]) => 
            // Remove duplicates and ensure not same as correct answer
            opt !== validateAndFixChoice(q.correctAnswer) && 
            arr.indexOf(opt) === index
          )
      }))
      .filter((q: any) => 
        // Ensure we still have at least 1 wrong option after filtering
        q.wrongOptions.length >= 1
      );

    if (validQuestions.length === 0) {
      throw new Error("No valid questions generated");
    }

    console.log(`✅ Successfully generated ${validQuestions.length} quiz questions`);

    return res.json({
      success: true,
      questions: validQuestions
    });

  } catch (error: any) {
    console.error("❌ Error generating quiz questions:", error.message);
    console.error("Full error:", error);
    
    // Provide more helpful error messages
    let errorMessage = "Failed to generate quiz questions";
    if (error.message?.includes("API key not valid") || error.message?.includes("API_KEY_INVALID")) {
      errorMessage = "Invalid API key. Please check your GEMINI_API_KEY in .env file and make sure it's valid. Get a new key from https://aistudio.google.com/apikey";
    } else if (error.message?.includes("quota") || error.message?.includes("rate limit")) {
      errorMessage = "API quota exceeded. Please try again later or check your API usage limits.";
    } else if (error.message?.includes("network") || error.message?.includes("timeout")) {
      errorMessage = "Network error. Please check your internet connection and try again.";
    } else {
      errorMessage = `Failed to generate quiz questions: ${error.message}`;
    }
    
    return res.status(500).json({
      success: false,
      message: errorMessage
    });
  }
});

// AI-powered lesson generation endpoint
app.post("/generate-lesson", async (req: Request, res: Response) => {
  const { topic, count = 1 } = req.body;

  if (!topic || topic.trim().length === 0) {
    return res.status(400).json({ 
      success: false, 
      message: "Topic is required" 
    });
  }

  if (!genAI) {
    return res.status(503).json({ 
      success: false, 
      message: "AI service is not available. Please set GEMINI_API_KEY in .env file." 
    });
  }

  try {
    // Get available model (tries different models until one works)
    const { model, modelName } = await getAvailableModel();
    console.log(`🤖 Using model: ${modelName} for lesson generation`);
    const numLessons = Math.min(Math.max(parseInt(count) || 1, 1), 5); // Between 1-5 lessons

    const prompt = `You are an AI assistant helping to create learning lessons for Expressora: An AI-Powered Mobile Application for American and Filipino Sign Language.

IMPORTANT: ALL lessons MUST be exclusively about American Sign Language (ASL) and/or Filipino Sign Language (FSL/FilSL). Do NOT generate content about spoken languages, general communication, or other topics unrelated to sign language.

CRITICAL: You MUST generate EXACTLY ${numLessons} comprehensive learning lesson(s) about "${topic}" that is STRICTLY related to American Sign Language (ASL) and/or Filipino Sign Language (FSL/FilSL). Do NOT generate fewer than ${numLessons} lessons. The JSON array MUST contain exactly ${numLessons} lesson objects.

Guidelines:
- MANDATORY: Every lesson MUST focus on ASL and/or FSL sign language learning
- Lessons should cover: ASL/FSL sign vocabulary, sign meanings, sign phrases, sign grammar, cultural context in deaf communities, differences between ASL and FSL, common ASL/FSL expressions, sign language structure, finger spelling, or sign language conversation
- Topics can include: Basic ASL/FSL greetings, ASL/FSL numbers, ASL/FSL alphabet, ASL vs FSL differences, ASL/FSL family signs, ASL/FSL emotions, ASL/FSL daily conversation, ASL/FSL grammar rules, etc.
- Make lessons relevant to the Expressora app context - a mobile application specifically for learning both ASL and FSL
- Keep lesson titles to maximum 100 characters (spaces allowed), and clearly indicate if it's about ASL, FSL, or both
- Keep lesson content under 5000 characters (comprehensive but concise)
- tryItems: ONE WORD ONLY, MAX 6 CHARACTERS, NO SPACES. Select 3-5 words from this list:
  * Greetings/Manners: "Hello" (5), "Thanks" (6), "Please" (6), "Sorry" (5), "Bye" (3), "Good" (4), "Nice" (4), "Hi" (2)
  * Family: "Family" (6), "Mother" (6), "Father" (6), "Sister" (6), "Child" (5), "Parent" (6), "Baby" (4), "Son" (3)
  * Emotions: "Happy" (5), "Sad" (3), "Angry" (5), "Love" (4), "Calm" (4), "Brave" (5), "Peace" (5), "Hope" (4), "Joy" (3), "Fear" (4)
  * Daily Activities: "Eat" (3), "Drink" (5), "Sleep" (5), "Work" (4), "Home" (4), "School" (6), "Study" (5), "Play" (4), "Read" (4), "Write" (5)
  * Actions: "Learn" (5), "Teach" (5), "Help" (4), "Stop" (4), "Wait" (4), "Come" (4), "Go" (2), "Stay" (4), "Run" (3), "Walk" (4)
  * Time: "Today" (5), "Now" (3), "Later" (5), "Soon" (4), "Early" (5), "Late" (4), "Night" (5), "Day" (3)
  * Common: "Yes" (3), "No" (2), "Water" (5), "Food" (4), "Friend" (6), "Person" (6), "People" (6), "Man" (3), "Woman" (5), "ASL" (3), "FSL" (3)
- Select 3-5 words from the appropriate category based on lesson topic. DO NOT create new words - only use words from the list above.

Return ONLY a valid JSON array with this exact structure:
[
  {
    "title": "Lesson Title Here (must be about ASL/FSL, max 100 chars, spaces allowed)",
    "content": "Comprehensive lesson content explaining the topic in detail. Include explanations, examples, and educational information about ASL/FSL sign language related to the topic. MUST be about sign language learning.",
    "tryItems": ["Hello", "Thanks", "Please", "Sorry", "Hi"]
  },
  ...
]

FINAL CRITICAL REMINDER: tryItems MUST be ONE WORD ONLY, MAX 6 CHARACTERS, NO SPACES. Select 3-5 words from the list above based on lesson topic. DO NOT create new words.

Make sure the JSON is valid and can be parsed. Do not include any explanation or markdown formatting, just the JSON array.`;

    console.log(`🤖 Generating ${numLessons} lesson(s) about "${topic}"...`);

    // Retry logic with exponential backoff for overloaded models
    let lastError: any = null;
    const maxRetries = 3;
    let result: any = null;
    let response: any = null;
    let text: string = "";

    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        result = await model.generateContent(prompt);
        response = await result.response;
        text = response.text();
        break; // Success, exit retry loop
      } catch (error: any) {
        lastError = error;
        const errorMessage = error.message || "";
        
        // Check if it's an overloaded/503 error
        if (errorMessage.includes("overloaded") || 
            errorMessage.includes("503") || 
            errorMessage.includes("Service Unavailable")) {
          
          if (attempt < maxRetries) {
            // Exponential backoff: 2s, 4s, 8s
            const delayMs = Math.pow(2, attempt) * 1000;
            console.log(`⚠️  Model overloaded (attempt ${attempt}/${maxRetries}). Retrying in ${delayMs/1000}s...`);
            await new Promise(resolve => setTimeout(resolve, delayMs));
            continue;
          } else {
            // Last attempt failed, try alternative model
            console.log(`⚠️  Model still overloaded after ${maxRetries} attempts. Trying alternative model...`);
            try {
              // Try gemini-1.5-flash as fallback
              const fallbackModel = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });
              result = await fallbackModel.generateContent(prompt);
              response = await result.response;
              text = response.text();
              console.log(`✅ Fallback model (gemini-1.5-flash) succeeded`);
              break;
            } catch (fallbackError: any) {
              throw new Error(`Model overloaded. Please try again in a few moments or reduce the number of lessons (currently ${numLessons}). Error: ${errorMessage}`);
            }
          }
        } else {
          // Not an overload error, throw immediately
          throw error;
        }
      }
    }

    if (!text) {
      throw lastError || new Error("Failed to generate content after retries");
    }

    // Extract JSON from response (remove markdown code blocks if present)
    let jsonText = text.trim();
    if (jsonText.startsWith("```json")) {
      jsonText = jsonText.replace(/```json\n?/g, "").replace(/```\n?/g, "");
    } else if (jsonText.startsWith("```")) {
      jsonText = jsonText.replace(/```\n?/g, "");
    }

    // Parse the JSON
    const lessons = JSON.parse(jsonText);

    if (!Array.isArray(lessons) || lessons.length === 0) {
      throw new Error("Invalid response format from AI");
    }

    console.log(`📊 AI generated ${lessons.length} lesson(s), requested ${numLessons}`);
    
    // Warn if AI generated fewer lessons than requested
    if (lessons.length < numLessons) {
      console.warn(`⚠️  AI only generated ${lessons.length} lesson(s) but ${numLessons} were requested`);
    }

    // Helper function to get relevant words based on lesson topic
    const getRelevantWords = (topic: string, title: string): string[] => {
      const topicLower = (topic + " " + title).toLowerCase();
      
      // Greetings/Manners category
      if (topicLower.includes("greet") || topicLower.includes("hello") || topicLower.includes("hi") || 
          topicLower.includes("thanks") || topicLower.includes("please") || topicLower.includes("sorry")) {
        return ["Hello", "Thanks", "Please", "Sorry", "Bye", "Good", "Nice", "Hi"];
      }
      
      // Family category
      if (topicLower.includes("family") || topicLower.includes("mother") || topicLower.includes("father") ||
          topicLower.includes("parent") || topicLower.includes("sister") || topicLower.includes("brother") ||
          topicLower.includes("child") || topicLower.includes("baby")) {
        return ["Family", "Mother", "Father", "Sister", "Child", "Parent", "Baby", "Son"];
      }
      
      // Emotions category
      if (topicLower.includes("emotion") || topicLower.includes("feel") || topicLower.includes("happy") ||
          topicLower.includes("sad") || topicLower.includes("angry") || topicLower.includes("love") ||
          topicLower.includes("calm") || topicLower.includes("brave")) {
        return ["Happy", "Sad", "Angry", "Love", "Calm", "Brave", "Peace", "Hope", "Joy", "Fear"];
      }
      
      // Daily Activities category
      if (topicLower.includes("daily") || topicLower.includes("activity") || topicLower.includes("eat") ||
          topicLower.includes("drink") || topicLower.includes("sleep") || topicLower.includes("work") ||
          topicLower.includes("home") || topicLower.includes("school") || topicLower.includes("study")) {
        return ["Eat", "Drink", "Sleep", "Work", "Home", "School", "Study", "Play", "Read", "Write"];
      }
      
      // Actions category
      if (topicLower.includes("action") || topicLower.includes("learn") || topicLower.includes("teach") ||
          topicLower.includes("help") || topicLower.includes("stop") || topicLower.includes("wait") ||
          topicLower.includes("come") || topicLower.includes("go")) {
        return ["Learn", "Teach", "Help", "Stop", "Wait", "Come", "Go", "Stay", "Run", "Walk"];
      }
      
      // Time category
      if (topicLower.includes("time") || topicLower.includes("today") || topicLower.includes("tomorrow") ||
          topicLower.includes("yesterday") || topicLower.includes("now") || topicLower.includes("later") ||
          topicLower.includes("night") || topicLower.includes("day")) {
        return ["Today", "Now", "Later", "Soon", "Early", "Late", "Night", "Day"];
      }
      
      // Default: Common/Greetings (most versatile)
      return ["Hello", "Thanks", "Please", "Sorry", "Hi", "Good", "Nice", "Yes", "No"];
    };

    // Helper function to validate and fix tryItems (one word, max 6 characters)
    const validateAndFixTryItem = (item: string, relevantWords: string[]): string => {
      if (!item) return relevantWords[0] || "Hello";
      
      let cleaned = item.trim();
      
      // Remove quotes, punctuation, and extra whitespace
      cleaned = cleaned.replace(/['"]/g, '').replace(/[^\w\s]/g, ' ').trim();
      
      // Extract first word only (handle sentences)
      const words = cleaned.split(/[\s\-]+/);
      const firstWord = words[0] || "";
      
      // If empty, use fallback from relevant words
      if (!firstWord || firstWord.length === 0) {
        return relevantWords[0] || "Hello";
      }
      
      // Approved list of words (max 6 chars)
      const approvedWords = [
        "Hello", "Thanks", "Please", "Sorry", "Bye", "Good", "Nice", "Hi",
        "Family", "Mother", "Father", "Sister", "Child", "Parent", "Baby", "Son",
        "Happy", "Sad", "Angry", "Love", "Calm", "Brave", "Peace", "Hope", "Joy", "Fear",
        "Eat", "Drink", "Sleep", "Work", "Home", "School", "Study", "Play", "Read", "Write",
        "Learn", "Teach", "Help", "Stop", "Wait", "Come", "Go", "Stay", "Run", "Walk",
        "Today", "Now", "Later", "Soon", "Early", "Late", "Night", "Day",
        "Yes", "No", "Water", "Food", "Friend", "Person", "People", "Man", "Woman", "ASL", "FSL"
      ];
      
      // Check if first word matches any approved word (case-insensitive)
      const matchedWord = approvedWords.find(w => w.toLowerCase() === firstWord.toLowerCase());
      if (matchedWord && matchedWord.length <= 6) {
        return matchedWord;
      }
      
      // Try to find shorter alternative for common AI mistakes (use relevant words from topic)
      const getRelevantAlternative = (word: string): string | null => {
        const lowerWord = word.toLowerCase();
        // Check if word starts with common patterns and map to relevant category
        if (lowerWord.startsWith("thank")) {
          return relevantWords.find(w => w.toLowerCase().includes("thank") || w === "Thanks") || relevantWords[0];
        } else if (lowerWord.startsWith("goodbye") || lowerWord.startsWith("bye")) {
          return relevantWords.find(w => w === "Bye" || w.toLowerCase().includes("bye")) || relevantWords[0];
        } else if (lowerWord.startsWith("good") || lowerWord.startsWith("nice")) {
          return relevantWords.find(w => w === "Good" || w === "Nice") || relevantWords[0];
        } else if (lowerWord.startsWith("practice") || lowerWord.startsWith("signing") || 
                   lowerWord.startsWith("engage") || lowerWord.startsWith("think") || 
                   lowerWord.startsWith("review") || lowerWord.startsWith("focus")) {
          // For practice/signing words, use first relevant word from topic
          return relevantWords[0];
        }
        return null;
      };
      
      const lowerFirst = firstWord.toLowerCase();
      const alternative = getRelevantAlternative(firstWord);
      if (alternative) {
        return alternative;
      }
      
      // If word is 6 chars or less and looks valid, check if it's close to an approved word
      if (firstWord.length <= 6) {
        // Check for partial matches in approved list
        const partialMatch = approvedWords.find(w => 
          w.toLowerCase().includes(lowerFirst) || lowerFirst.includes(w.toLowerCase())
        );
        if (partialMatch && partialMatch.length <= 6) {
          return partialMatch;
        }
        // If no match found, use relevant word from topic category
        const relevantMatch = relevantWords.find(w => w.length <= 6);
        if (relevantMatch) {
          console.warn(`⚠️ TryItem "${item}" (${firstWord}) is not in approved list. Using relevant word: "${relevantMatch}"`);
          return relevantMatch;
        }
        // Last resort: use first relevant word
        return relevantWords[0] || "Hello";
      }
      
      // If word is longer than 6 chars, use relevant word from topic (NO TRUNCATION)
      const relevantMatch = relevantWords.find(w => w.length <= 6);
      if (relevantMatch) {
        console.warn(`⚠️ TryItem "${item}" (${firstWord}, ${firstWord.length} chars) is too long. Using relevant word: "${relevantMatch}"`);
        return relevantMatch;
      }
      return relevantWords[0] || "Hello";
    };

    // Validate each lesson has required fields and fix tryItems
    const validLessons = lessons.map((l: any) => {
      // Get relevant words based on lesson topic and title
      const relevantWords = getRelevantWords(topic, l.title || "");
      
      // Fix tryItems before validation (pass relevant words for context-aware fallbacks)
      if (Array.isArray(l.tryItems)) {
        l.tryItems = l.tryItems
          .map((item: string) => validateAndFixTryItem(item, relevantWords))
          .filter((item: string) => item.length > 0) // Remove empty items
          .slice(0, 5); // Limit to 5
      }
      
      return l;
    }).filter((l: any) => {
      const titleTrimmed = l.title?.trim() || "";
      const hasValidTitle = l.title && 
        titleTrimmed.length > 0 &&
        titleTrimmed.length <= 100; // Max 100 characters, spaces allowed
      
      // Validate try items: one word, max 6 chars, no spaces (after fixing)
      const hasValidTryItems = Array.isArray(l.tryItems) && 
        l.tryItems.length >= 3 &&
        l.tryItems.length <= 5 &&
        l.tryItems.every((item: string) => {
          const itemTrimmed = item?.trim() || "";
          return itemTrimmed.length > 0 &&
            itemTrimmed.length <= 6 &&
            !itemTrimmed.includes(" "); // One word only, no spaces
        });
      
      return hasValidTitle &&
        l.content && 
        l.content.trim().length > 0 &&
        l.content.trim().length <= 5000 &&
        hasValidTryItems;
    });

    if (validLessons.length === 0) {
      throw new Error("No valid lessons generated");
    }

    console.log(`✅ Successfully validated ${validLessons.length} lesson(s) out of ${lessons.length} generated (requested: ${numLessons})`);

    return res.json({
      success: true,
      lessons: validLessons.map((l: any) => {
        const titleTrimmed = l.title.trim();
        // Ensure title is max 100 chars (additional validation)
        const finalTitle = titleTrimmed.substring(0, 100);
        // Try items are already validated and fixed above, just ensure they meet final criteria
        const finalTryItems = (l.tryItems || [])
          .filter((item: string) => item && item.length > 0 && item.length <= 6 && !item.includes(" "))
          .slice(0, 5);
        return {
          title: finalTitle,
          content: l.content.trim(),
          tryItems: finalTryItems
        };
      })
    });

  } catch (error: any) {
    console.error("❌ Error generating lesson:", error.message);
    
    // Provide more helpful error messages
    let errorMessage = "Failed to generate lesson";
    if (error.message?.includes("overloaded") || error.message?.includes("503") || error.message?.includes("Service Unavailable")) {
      errorMessage = `Model is currently overloaded. Please try again in a few moments or reduce the number of lessons (currently ${count}). The AI service is experiencing high traffic.`;
    } else if (error.message?.includes("API key not valid") || error.message?.includes("API_KEY_INVALID")) {
      errorMessage = "Invalid API key. Please check your GEMINI_API_KEY in .env file and make sure it's valid. Get a new key from https://aistudio.google.com/apikey";
    } else if (error.message?.includes("quota") || error.message?.includes("rate limit")) {
      errorMessage = "API quota exceeded. Please try again later or check your API usage limits.";
    } else if (error.message?.includes("network") || error.message?.includes("timeout")) {
      errorMessage = "Network error. Please check your internet connection and try again.";
    } else {
      errorMessage = `Failed to generate lesson: ${error.message}`;
    }
    
    return res.status(500).json({
      success: false,
      message: errorMessage
    });
  }
});

// Cache for working model to avoid testing every time
let cachedModel: { model: any; modelName: string } | null = null;

// Helper function to list available models using REST API
async function listAvailableModels(): Promise<string[]> {
  const apiKey = process.env.GEMINI_API_KEY?.trim();
  if (!apiKey) return ["gemini-pro"];
  
  return new Promise((resolve) => {
    const url = `https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey}`;
    https.get(url, (res) => {
      let data = "";
      res.on("data", (chunk) => { data += chunk; });
      res.on("end", () => {
        try {
          const json = JSON.parse(data);
          if (json.models) {
            const modelNames = json.models
              .filter((m: any) => m.name && m.supportedGenerationMethods?.includes("generateContent"))
              .map((m: any) => m.name.replace("models/", ""));
            if (modelNames.length > 0) {
              console.log(`✅ Found ${modelNames.length} available models:`, modelNames);
              resolve(modelNames);
              return;
            }
          }
        } catch (error) {
          console.warn("⚠️  Could not parse models list");
        }
        resolve(["gemini-pro"]);
      });
    }).on("error", () => {
      console.warn("⚠️  Could not list models, using defaults");
      resolve(["gemini-pro"]);
    });
  });
}

// Helper function to get available model
async function getAvailableModel(): Promise<{ model: any; modelName: string }> {
  if (!genAI) throw new Error("Gemini AI not initialized");
  
  // Return cached model if available (but clear if it fails later)
  // if (cachedModel) {
  //   return cachedModel;
  // }
  
  // Use gemini-pro as default since it's the most compatible with v1beta API
  // Other models like gemini-1.5-flash might not be available for v1beta
  const defaultModelNames = ["gemini-pro"];
  
  // Try to list available models from API (optional)
  let modelNames: string[] = defaultModelNames;
  try {
    const listedModels = await listAvailableModels();
    // Prefer gemini-pro if it's in the list, otherwise use listed models
    if (listedModels.includes("gemini-pro")) {
      modelNames = ["gemini-pro", ...listedModels.filter((m: string) => m !== "gemini-pro")];
    } else if (listedModels.length > 0) {
      modelNames = listedModels;
    }
    console.log(`📋 Using models: ${modelNames.join(", ")}`);
  } catch (error: any) {
    console.warn("⚠️  Could not list models, using default: gemini-pro");
  }
  
  // Test each model by making a small API call
  console.log(`🔍 Testing ${modelNames.length} model(s)...`);
  for (const modelName of modelNames) {
    try {
      console.log(`🧪 Testing model: ${modelName}...`);
      const model = genAI.getGenerativeModel({ model: modelName });
      // Test with a simple call to see if model works
      const testResult = await model.generateContent("test");
      const response = await testResult.response;
      await response.text();
      // If successful, cache and return
      cachedModel = { model, modelName };
      console.log(`✅ Found working model: ${modelName}`);
      return cachedModel;
    } catch (error: any) {
      console.log(`❌ Model ${modelName} failed: ${error.message?.substring(0, 100)}`);
      // Clear cache if it was using a bad model
      if (cachedModel?.modelName === modelName) {
        cachedModel = null;
      }
      continue;
    }
  }
  
  // If all fail, throw error with helpful message
  throw new Error("No available Gemini models found. Please check your API key permissions, enable the Generative AI API in Google Cloud Console, or try updating @google/generative-ai package.");
}

// Endpoint to list available models
app.get("/list-models", async (req: Request, res: Response) => {
  try {
    const models = await listAvailableModels();
    return res.json({
      success: true,
      models: models
    });
  } catch (error: any) {
    return res.status(500).json({
      success: false,
      message: `Failed to list models: ${error.message}`
    });
  }
});

// Test endpoint to verify API key and list available models
app.get("/test-gemini", async (req: Request, res: Response) => {
  if (!genAI) {
    return res.status(503).json({
      success: false,
      message: "Gemini AI not initialized. Check GEMINI_API_KEY in .env file."
    });
  }

  try {
    // Clear cache to force re-detection
    cachedModel = null;
    
    const { model, modelName } = await getAvailableModel();
    const result = await model.generateContent("Say 'Hello' if you can read this.");
    const response = await result.response;
    const text = response.text();
    
    return res.json({
      success: true,
      message: "API key is valid!",
      model: modelName,
      response: text
    });
  } catch (error: any) {
    return res.status(500).json({
      success: false,
      message: `API key test failed: ${error.message}`,
      hint: "Get a new API key from https://aistudio.google.com/apikey or check model availability"
    });
  }
});

// YouTube API endpoints
const YOUTUBE_API_KEY = process.env.YOUTUBE_API_KEY?.trim();
const YOUTUBE_API_BASE = "https://www.googleapis.com/youtube/v3";

if (YOUTUBE_API_KEY) {
  console.log(`✅ YouTube API initialized`);
  console.log(`🔑 API Key: ${YOUTUBE_API_KEY.substring(0, 10)}...${YOUTUBE_API_KEY.substring(YOUTUBE_API_KEY.length - 4)}`);
} else {
  console.warn(`⚠️  YouTube API key not found. Tutorial videos will not work.`);
  console.warn(`💡 Add YOUTUBE_API_KEY to your .env file to enable YouTube features.`);
}

// Get videos from a YouTube channel
app.get("/youtube/videos", async (req: Request, res: Response) => {
  const { channelId, maxResults = 50 } = req.query;

  if (!YOUTUBE_API_KEY) {
    return res.status(503).json({
      success: false,
      message: "YouTube API key not configured. Please set YOUTUBE_API_KEY in .env file."
    });
  }

  if (!channelId || typeof channelId !== "string") {
    return res.status(400).json({
      success: false,
      message: "channelId is required"
    });
  }

  try {
    console.log(`📹 Fetching videos from channel: ${channelId} (max: ${maxResults})`);
    
    // First, get the uploads playlist ID from the channel
    const channelUrl = `${YOUTUBE_API_BASE}/channels?part=contentDetails&id=${channelId}&key=${YOUTUBE_API_KEY}`;
    const channelRes = await fetch(channelUrl);
    
    if (!channelRes.ok) {
      const errorData = await channelRes.json().catch(() => ({}));
      console.error(`❌ YouTube API error (channel): ${channelRes.status}`, errorData);
      return res.status(channelRes.status).json({
        success: false,
        message: errorData.error?.message || `Failed to fetch channel: ${channelRes.statusText}`
      });
    }

    const channelData = await channelRes.json();
    
    if (!channelData.items || channelData.items.length === 0) {
      console.error(`❌ Channel not found: ${channelId}`);
      return res.status(404).json({
        success: false,
        message: "Channel not found"
      });
    }

    const uploadsPlaylistId = channelData.items[0].contentDetails?.relatedPlaylists?.uploads;
    
    if (!uploadsPlaylistId) {
      console.error(`❌ No uploads playlist found for channel: ${channelId}`);
      return res.status(404).json({
        success: false,
        message: "Channel has no uploads playlist"
      });
    }

    // Get videos from the uploads playlist
    const videosUrl = `${YOUTUBE_API_BASE}/playlistItems?part=snippet&playlistId=${uploadsPlaylistId}&maxResults=${Math.min(Number(maxResults) || 50, 50)}&key=${YOUTUBE_API_KEY}`;
    const videosRes = await fetch(videosUrl);
    
    if (!videosRes.ok) {
      const errorData = await videosRes.json().catch(() => ({}));
      console.error(`❌ YouTube API error (videos): ${videosRes.status}`, errorData);
      return res.status(videosRes.status).json({
        success: false,
        message: errorData.error?.message || `Failed to fetch videos: ${videosRes.statusText}`
      });
    }

    const videosData = await videosRes.json();
    
    const videos = (videosData.items || []).map((item: any) => {
      const snippet = item.snippet;
      const videoId = snippet.resourceId?.videoId || "";
      const thumbnails = snippet.thumbnails || {};
      const thumbnailUrl = thumbnails.high?.url || thumbnails.medium?.url || thumbnails.default?.url || "";
      
      return {
        id: videoId,
        title: snippet.title || "",
        description: snippet.description || "",
        thumbnailUrl: thumbnailUrl,
        videoUrl: `https://www.youtube.com/watch?v=${videoId}`,
        publishedAt: snippet.publishedAt || "",
        channelTitle: snippet.channelTitle || ""
      };
    });

    console.log(`✅ Successfully fetched ${videos.length} videos from channel ${channelId}`);
    
    return res.json({
      success: true,
      videos: videos
    });
  } catch (error: any) {
    console.error(`❌ Error fetching YouTube videos:`, error.message);
    return res.status(500).json({
      success: false,
      message: `Failed to fetch videos: ${error.message}`
    });
  }
});

// Get playlists from a YouTube channel
app.get("/youtube/playlists", async (req: Request, res: Response) => {
  const { channelId, maxResults = 50 } = req.query;

  if (!YOUTUBE_API_KEY) {
    return res.status(503).json({
      success: false,
      message: "YouTube API key not configured. Please set YOUTUBE_API_KEY in .env file."
    });
  }

  if (!channelId || typeof channelId !== "string") {
    return res.status(400).json({
      success: false,
      message: "channelId is required"
    });
  }

  try {
    console.log(`📋 Fetching playlists from channel: ${channelId} (max: ${maxResults})`);
    
    const playlistsUrl = `${YOUTUBE_API_BASE}/playlists?part=snippet,contentDetails&channelId=${channelId}&maxResults=${Math.min(Number(maxResults) || 50, 50)}&key=${YOUTUBE_API_KEY}`;
    const playlistsRes = await fetch(playlistsUrl);
    
    if (!playlistsRes.ok) {
      const errorData = await playlistsRes.json().catch(() => ({}));
      console.error(`❌ YouTube API error (playlists): ${playlistsRes.status}`, errorData);
      return res.status(playlistsRes.status).json({
        success: false,
        message: errorData.error?.message || `Failed to fetch playlists: ${playlistsRes.statusText}`
      });
    }

    const playlistsData = await playlistsRes.json();
    
    const playlists = (playlistsData.items || []).map((item: any) => {
      const snippet = item.snippet;
      const thumbnails = snippet.thumbnails || {};
      const thumbnailUrl = thumbnails.high?.url || thumbnails.medium?.url || thumbnails.default?.url || "";
      
      return {
        playlistId: item.id,
        title: snippet.title || "",
        description: snippet.description || "",
        thumbnailUrl: thumbnailUrl,
        playlistUrl: `https://www.youtube.com/playlist?list=${item.id}`,
        publishedAt: snippet.publishedAt || "",
        channelTitle: snippet.channelTitle || "",
        itemCount: item.contentDetails?.itemCount || 0
      };
    });

    console.log(`✅ Successfully fetched ${playlists.length} playlists from channel ${channelId}`);
    
    return res.json({
      success: true,
      playlists: playlists
    });
  } catch (error: any) {
    console.error(`❌ Error fetching YouTube playlists:`, error.message);
    return res.status(500).json({
      success: false,
      message: `Failed to fetch playlists: ${error.message}`
    });
  }
});

// Get videos from a YouTube playlist
app.get("/youtube/playlist-videos", async (req: Request, res: Response) => {
  const { playlistId, maxResults = 50 } = req.query;

  if (!YOUTUBE_API_KEY) {
    return res.status(503).json({
      success: false,
      message: "YouTube API key not configured. Please set YOUTUBE_API_KEY in .env file."
    });
  }

  if (!playlistId || typeof playlistId !== "string") {
    return res.status(400).json({
      success: false,
      message: "playlistId is required"
    });
  }

  try {
    console.log(`📹 Fetching videos from playlist: ${playlistId} (max: ${maxResults})`);
    
    const videosUrl = `${YOUTUBE_API_BASE}/playlistItems?part=snippet&playlistId=${playlistId}&maxResults=${Math.min(Number(maxResults) || 50, 50)}&key=${YOUTUBE_API_KEY}`;
    const videosRes = await fetch(videosUrl);
    
    if (!videosRes.ok) {
      const errorData = await videosRes.json().catch(() => ({}));
      console.error(`❌ YouTube API error (playlist videos): ${videosRes.status}`, errorData);
      return res.status(videosRes.status).json({
        success: false,
        message: errorData.error?.message || `Failed to fetch playlist videos: ${videosRes.statusText}`
      });
    }

    const videosData = await videosRes.json();
    
    const videos = (videosData.items || []).map((item: any, index: number) => {
      const snippet = item.snippet;
      const videoId = snippet.resourceId?.videoId || "";
      const thumbnails = snippet.thumbnails || {};
      const thumbnailUrl = thumbnails.high?.url || thumbnails.medium?.url || thumbnails.default?.url || "";
      
      return {
        videoId: videoId,
        title: snippet.title || "",
        description: snippet.description || "",
        thumbnailUrl: thumbnailUrl,
        publishedAt: snippet.publishedAt || "",
        channelTitle: snippet.channelTitle || "",
        position: index
      };
    });

    console.log(`✅ Successfully fetched ${videos.length} videos from playlist ${playlistId}`);
    
    return res.json({
      success: true,
      videos: videos
    });
  } catch (error: any) {
    console.error(`❌ Error fetching YouTube playlist videos:`, error.message);
    return res.status(500).json({
      success: false,
      message: `Failed to fetch playlist videos: ${error.message}`
    });
  }
});

// Alternative endpoint for playlist videos (used by some Android code)
app.get("/youtube-playlist-videos", async (req: Request, res: Response) => {
  const { playlistId } = req.query;

  if (!YOUTUBE_API_KEY) {
    return res.status(503).json({
      success: false,
      message: "YouTube API key not configured. Please set YOUTUBE_API_KEY in .env file."
    });
  }

  if (!playlistId || typeof playlistId !== "string") {
    return res.status(400).json({
      success: false,
      message: "playlistId is required"
    });
  }

  try {
    console.log(`📹 Fetching videos from playlist: ${playlistId}`);
    
    const videosUrl = `${YOUTUBE_API_BASE}/playlistItems?part=snippet&playlistId=${playlistId}&maxResults=50&key=${YOUTUBE_API_KEY}`;
    const videosRes = await fetch(videosUrl);
    
    if (!videosRes.ok) {
      const errorData = await videosRes.json().catch(() => ({}));
      console.error(`❌ YouTube API error (playlist videos): ${videosRes.status}`, errorData);
      return res.status(videosRes.status).json({
        success: false,
        message: errorData.error?.message || `Failed to fetch playlist videos: ${videosRes.statusText}`
      });
    }

    const videosData = await videosRes.json();
    
    const videos = (videosData.items || []).map((item: any, index: number) => {
      const snippet = item.snippet;
      const videoId = snippet.resourceId?.videoId || "";
      const thumbnails = snippet.thumbnails || {};
      const thumbnailUrl = thumbnails.high?.url || thumbnails.medium?.url || thumbnails.default?.url || "";
      
      return {
        videoId: videoId,
        title: snippet.title || "",
        description: snippet.description || "",
        thumbnailUrl: thumbnailUrl,
        publishedAt: snippet.publishedAt || "",
        channelTitle: snippet.channelTitle || "",
        position: index
      };
    });

    console.log(`✅ Successfully fetched ${videos.length} videos from playlist ${playlistId}`);
    
    return res.json({
      success: true,
      videos: videos,
      playlistId: playlistId,
      totalVideos: videos.length
    });
  } catch (error: any) {
    console.error(`❌ Error fetching YouTube playlist videos:`, error.message);
    return res.status(500).json({
      success: false,
      message: `Failed to fetch playlist videos: ${error.message}`
    });
  }
});

// Get or update YouTube playlist configuration
app.get("/youtube-playlist-config", async (req: Request, res: Response) => {
  try {
    // Get configuration from Firestore
    const configDoc = await db.collection("youtube_config").doc("default").get();
    
    if (configDoc.exists) {
      const config = configDoc.data();
      console.log(`✅ Retrieved YouTube config from Firestore`);
      return res.json({
        success: true,
        playlistId: config?.playlistId || null,
        channelId: config?.channelId || null,
        organizationName: config?.organizationName || null,
        useChannelVideos: config?.useChannelVideos || false
      });
    } else {
      // Return default/empty config
      console.log(`ℹ️  No YouTube config found, returning defaults`);
      return res.json({
        success: true,
        playlistId: null,
        channelId: null,
        organizationName: null,
        useChannelVideos: false
      });
    }
  } catch (error: any) {
    console.error(`❌ Error fetching YouTube config:`, error.message);
    return res.status(500).json({
      success: false,
      message: `Failed to fetch config: ${error.message}`
    });
  }
});

app.post("/youtube-playlist-config", async (req: Request, res: Response) => {
  const { playlistId, channelId, organizationName, useChannelVideos } = req.body;

  try {
    await db.collection("youtube_config").doc("default").set({
      playlistId: playlistId || null,
      channelId: channelId || null,
      organizationName: organizationName || null,
      useChannelVideos: useChannelVideos || false,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    console.log(`✅ Updated YouTube config: playlistId=${playlistId}, channelId=${channelId}`);
    
    return res.json({
      success: true,
      message: "Configuration updated successfully"
    });
  } catch (error: any) {
    console.error(`❌ Error updating YouTube config:`, error.message);
    return res.status(500).json({
      success: false,
      message: `Failed to update config: ${error.message}`
    });
  }
});

app.listen(PORT, "0.0.0.0", () => {
  console.log(`🚀 Expressora Server running at http://localhost:${PORT}`);
  if (genAI) {
    console.log(`✅ AI features enabled. Test API key: http://localhost:${PORT}/test-gemini`);
  } else {
    console.log(`⚠️  AI features disabled. Add GEMINI_API_KEY to .env file.`);
  }
  if (YOUTUBE_API_KEY) {
    console.log(`✅ YouTube API enabled. Test: http://localhost:${PORT}/youtube/videos?channelId=UCuzuc0P8L1fVIeeZrNLnkQA&maxResults=5`);
  } else {
    console.log(`⚠️  YouTube API disabled. Add YOUTUBE_API_KEY to .env file.`);
  }
});
