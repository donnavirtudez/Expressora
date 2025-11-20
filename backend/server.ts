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
      EASY: "Beginner level - Simple, common sign language vocabulary and basic phrases. Questions should be straightforward with clear visual signs.",
      MEDIUM: "Intermediate level - More complex signs, phrases, and common expressions. Questions require understanding of context.",
      DIFFICULT: "Advanced level - Complex signs, idioms, and nuanced expressions. Questions require deeper understanding and interpretation.",
      PRO: "Expert level - Master-level signs, technical vocabulary, and sophisticated expressions. Questions are challenging and require expertise."
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
- Keep answers under 100 characters each
- Wrong options should be plausible but clearly incorrect
- Wrong options should not be duplicates of each other or the correct answer
- Make questions relevant to the Expressora app context - a mobile application for learning both ASL and FSL through visual sign identification

Return ONLY a valid JSON array with this exact structure:
[
  {
    "question": "Question text here (phrased as if user is looking at a sign image)",
    "correctAnswer": "Correct answer here (the meaning/word/phrase/sign language)",
    "wrongOptions": ["Wrong option 1", "Wrong option 2", "Wrong option 3"]
  },
  ...
]

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

    // Validate each question has required fields
    const validQuestions = questions.filter((q: any) => 
      q.question && 
      q.correctAnswer && 
      Array.isArray(q.wrongOptions) && 
      q.wrongOptions.length >= 1 &&
      q.wrongOptions.length <= 3
    );

    if (validQuestions.length === 0) {
      throw new Error("No valid questions generated");
    }

    console.log(`✅ Successfully generated ${validQuestions.length} quiz questions`);

    return res.json({
      success: true,
      questions: validQuestions.map((q: any) => ({
        question: q.question.trim(),
        correctAnswer: q.correctAnswer.trim(),
        wrongOptions: q.wrongOptions.map((opt: string) => opt.trim()).slice(0, 3)
      }))
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

Generate exactly ${numLessons} comprehensive learning lesson(s) about "${topic}" that is STRICTLY related to American Sign Language (ASL) and/or Filipino Sign Language (FSL/FilSL).

Guidelines:
- MANDATORY: Every lesson MUST focus on ASL and/or FSL sign language learning
- Lessons should cover: ASL/FSL sign vocabulary, sign meanings, sign phrases, sign grammar, cultural context in deaf communities, differences between ASL and FSL, common ASL/FSL expressions, sign language structure, finger spelling, or sign language conversation
- Topics can include: Basic ASL/FSL greetings, ASL/FSL numbers, ASL/FSL alphabet, ASL vs FSL differences, ASL/FSL family signs, ASL/FSL emotions, ASL/FSL daily conversation, ASL/FSL grammar rules, etc.
- Make lessons relevant to the Expressora app context - a mobile application specifically for learning both ASL and FSL
- Keep lesson titles under 200 characters and clearly indicate if it's about ASL, FSL, or both
- Keep lesson content under 5000 characters (comprehensive but concise)
- Each lesson should have 3-5 "Try It Out" items (practical sign language exercises, practice suggestions, or hands-on activities)
- Try items should be actionable sign language practice activities related to the lesson topic
- Focus on practical, hands-on sign language learning
- Include specific sign language examples, sign descriptions, or sign language concepts

Return ONLY a valid JSON array with this exact structure:
[
  {
    "title": "Lesson title here (must be about ASL/FSL)",
    "content": "Comprehensive lesson content explaining the topic in detail. Include explanations, examples, and educational information about ASL/FSL sign language related to the topic. MUST be about sign language learning.",
    "tryItems": ["Try item 1 (sign language practice)", "Try item 2 (sign language practice)", "Try item 3 (sign language practice)", "Try item 4 (sign language practice)", "Try item 5 (sign language practice)"]
  },
  ...
]

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

    // Validate each lesson has required fields
    const validLessons = lessons.filter((l: any) => 
      l.title && 
      l.title.trim().length > 0 &&
      l.title.trim().length <= 200 &&
      l.content && 
      l.content.trim().length > 0 &&
      l.content.trim().length <= 5000 &&
      Array.isArray(l.tryItems) && 
      l.tryItems.length >= 3 &&
      l.tryItems.length <= 5
    );

    if (validLessons.length === 0) {
      throw new Error("No valid lessons generated");
    }

    console.log(`✅ Successfully generated ${validLessons.length} lesson(s)`);

    return res.json({
      success: true,
      lessons: validLessons.map((l: any) => ({
        title: l.title.trim(),
        content: l.content.trim(),
        tryItems: l.tryItems.map((item: string) => item.trim()).slice(0, 5)
      }))
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

app.listen(PORT, "0.0.0.0", () => {
  console.log(`🚀 Expressora Server running at http://localhost:${PORT}`);
  if (genAI) {
    console.log(`✅ AI features enabled. Test API key: http://localhost:${PORT}/test-gemini`);
  } else {
    console.log(`⚠️  AI features disabled. Add GEMINI_API_KEY to .env file.`);
  }
});
