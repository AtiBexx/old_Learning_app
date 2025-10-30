
#include <jni.h>
#include <string>
#include <vector>
#include "nlohmann/json.hpp"

using Json = nlohmann::json;
// NOLINTNEXTLINE(readability-identifier-naming)
extern "C" JNIEXPORT jstring
Java_com_attibexx_old_1learning_1app_json_JsonSaver_createJsonStringWithCpp(
	JNIEnv* env,
	jobject /* this */,
	jobject questionAnswerPairsList) {

	std::vector<Json> jsonObjects;

	// Osztályok és metódusok lekérdezése a cikluson kívül
	jclass listClass = env->GetObjectClass(questionAnswerPairsList);
	jmethodID listGetMethod = env->GetMethodID(listClass, "get", "(I)Ljava/lang/Object;");
	jmethodID listSizeMethod = env->GetMethodID(listClass, "size", "()I");

	jclass questionAnswerClass = env->FindClass("com/attibexx/old_learning_app/QuestionAnswer");
	if (questionAnswerClass == nullptr) {
		env->ThrowNew(env->FindClass("java/lang/ClassNotFoundException"), "QuestionAnswer class not found");
		return nullptr;
	}
	jfieldID questionField = env->GetFieldID(questionAnswerClass, "question", "Ljava/lang/String;");
	jfieldID rightAnswersField = env->GetFieldID(questionAnswerClass, "rightAnswers", "Ljava/util/ArrayList;");
	jfieldID answeredField = env->GetFieldID(questionAnswerClass, "answered", "Z");

	jclass arrayListClass = env->FindClass("java/util/ArrayList");
	jmethodID arrayListGetMethod = env->GetMethodID(arrayListClass, "get", "(I)Ljava/lang/Object;");
	jmethodID arrayListSizeMethod = env->GetMethodID(arrayListClass, "size", "()I");

	int listSize = env->CallIntMethod(questionAnswerPairsList, listSizeMethod);

	// Lista feldolgozása
	for (int i = 0; i < listSize; i++) {
		jobject questionAnswerObject = env->CallObjectMethod(questionAnswerPairsList, listGetMethod, i);

		jstring jQuestion = static_cast<jstring>(env->GetObjectField(questionAnswerObject, questionField));
		jobject jRightAnswersList = env->GetObjectField(questionAnswerObject, rightAnswersField);
		jboolean jAnswered = env->GetBooleanField(questionAnswerObject, answeredField);

		const char* questionChars = env->GetStringUTFChars(jQuestion, nullptr);
		std::string stdQuestion(questionChars);
		env->ReleaseStringUTFChars(jQuestion, questionChars);

		std::vector<std::string> stdRightAnswers;
		if (jRightAnswersList != nullptr) {
			int answersListSize = env->CallIntMethod(jRightAnswersList, arrayListSizeMethod);
			for (int j = 0; j < answersListSize; j++) {
				jstring jAnswer = static_cast<jstring>(env->CallObjectMethod(jRightAnswersList, arrayListGetMethod, j));
				const char* answerChars = env->GetStringUTFChars(jAnswer, nullptr);
				stdRightAnswers.push_back(std::string(answerChars));
				env->ReleaseStringUTFChars(jAnswer, answerChars);
				env->DeleteLocalRef(jAnswer);
			}
		}

		// JSON objektum építése
		Json jObj;
		jObj["question"] = stdQuestion;
		jObj["rightAnswers"] = stdRightAnswers;
		jObj["answered"] = static_cast<bool>(jAnswered);
		jsonObjects.push_back(jObj);

		// Cikluson belüli referenciák felszabadítása
		env->DeleteLocalRef(questionAnswerObject);
		env->DeleteLocalRef(jQuestion);
		if (jRightAnswersList != nullptr) {
			env->DeleteLocalRef(jRightAnswersList);
		}
	} // <-- A FOR CIKLUS ZÁRÓJELE ITT VAN A HELYES HELYEN

	// Cikluson kívüli, globálisabb referenciák felszabadítása
	env->DeleteLocalRef(listClass);
	env->DeleteLocalRef(questionAnswerClass);
	env->DeleteLocalRef(arrayListClass);

	// JSON string generálása A TELJES FELDOLGOZÁS UTÁN
	try {
		std::string resultString = Json(jsonObjects).dump(4);
		return env->NewStringUTF(resultString.c_str());
	} catch (const Json::exception& e) {
		std::string errorMsg = "JSON serialization failed: " + std::string(e.what());
		env->ThrowNew(env->FindClass("java/lang/RuntimeException"), errorMsg.c_str());
		return nullptr;
	}
}

// ÚJ FÜGGVÉNY: A JSON string beolvasása
// NEW FUNCTION: Read JSON string

// NOLINTNEXTLINE(readability-identifier-naming)
extern "C" JNIEXPORT jobject
Java_com_attibexx_old_1learning_1app_json_CppJsonLoader_readJsonStringWithCpp(
	JNIEnv *env,
	jobject /* this - CppJsonLoader instance */,
	jstring jsonString) {

	// A bejövő Java String átalakítása C++ stringgé
	// Converting an incoming Java String to a C++ string
	const char *jsonChars = env->GetStringUTFChars(jsonString, nullptr);
	std::string cppJsonString(jsonChars);
	env->ReleaseStringUTFChars(jsonString, jsonChars);

	// ArrayList, amibe gyűjteni fogunk
	// ArrayList, which we will collect into
	jclass arrayListClass = env->FindClass("java/util/ArrayList");
	if (arrayListClass == nullptr) return nullptr; // Hiba
	jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "()V");
	jmethodID arrayListAddMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
	jobject resultList = env->NewObject(arrayListClass, arrayListConstructor); // Létrehozzuk az üres listát

	// QuestionAnswer osztály és a konstruktora
	// QuestionAnswer class and its constructor
	jclass questionAnswerClass = env->FindClass("com/attibexx/old_learning_app/QuestionAnswer");
	if (questionAnswerClass == nullptr) return nullptr; // Hiba

	// Fontos a konstruktor szignatúrájának pontosan meg kell egyeznie a Kotlin data class-szal!
	// It is important that the constructor signature exactly matches the Kotlin data class!
	// (Ljava/lang/String;Ljava/util/List;Z) -> (String, List, Boolean)
	jmethodID questionAnswerConstructor = env->GetMethodID(questionAnswerClass, "<init>", "(Ljava/lang/String;Ljava/util/List;Z)V");
	if (questionAnswerConstructor == nullptr) return nullptr; // Hiba

	try {
		// A C++ string feldolgozása nlohmann::json objektummá
		// Converting the C++ string to a nlohmann::json object
		auto jsonArray = Json::parse(cppJsonString);

		// Végigiterálunk a JSON tömbön
		// Iterating through the JSON array
		for (const auto& jObj : jsonArray) {
			// Adatok kiolvasása a JSON objektumból
			std::string questionStr = jObj.at("question").get<std::string>();
			bool answeredBool = jObj.at("answered").get<bool>();
			std::vector<std::string> rightAnswersVec = jObj.at("rightAnswers").get<std::vector<std::string>>();

			// Java/Kotlin objektumokká alakítás (C++ -> Java)
			// Converting to Java/Kotlin objects (C++ -> Java)
			// `question` C++ string -> Java String
			jstring jQuestion = env->NewStringUTF(questionStr.c_str());

			// `answered` C++ bool -> Java boolean
			jboolean jAnswered = static_cast<jboolean>(answeredBool);

			// `rightAnswers` C++ vector -> Java ArrayList<String>
			jobject jRightAnswersList = env->NewObject(arrayListClass, arrayListConstructor);
			for (const auto& answer : rightAnswersVec) {
				jstring jAnswer = env->NewStringUTF(answer.c_str());
				env->CallBooleanMethod(jRightAnswersList, arrayListAddMethod, jAnswer);
				env->DeleteLocalRef(jAnswer); // Memóriakezelés
			}

			// Hozzuk létre az új QuestionAnswer objektumot a C++-ból kinyert adatokkal
			// Create the new QuestionAnswer object with the data extracted from C++
			jobject newQuestionAnswer = env->NewObject(questionAnswerClass, questionAnswerConstructor,
				jQuestion, jRightAnswersList, jAnswered);

			// Adjuk hozzá a fő listához
			// Add to the main list
			env->CallBooleanMethod(resultList, arrayListAddMethod, newQuestionAnswer);

			// Memóriakezelés (a ciklusban létrehozott helyi referenciák törlése)
			// Memory management (deleting local references created in the loop)
			env->DeleteLocalRef(jQuestion);
			env->DeleteLocalRef(jRightAnswersList);
			env->DeleteLocalRef(newQuestionAnswer);
		}
	} catch (const Json::exception& e) {
		// Hiba esetén dobunk egy Java/Kotlin kivételt a hibaüzenettel
		// If an error occurs, we throw a Java/Kotlin exception with the error message
		std::string errorMsg = "C++ JSON parsing failed: " + std::string(e.what());
		env->ThrowNew(env->FindClass("java/lang/RuntimeException"), errorMsg.c_str());
		return nullptr; // A hiba jelzése
	}

	// A Cikluson kívüli, globálisabb referenciák felszabadítása
	// Memory management (deleting global references)
	env->DeleteLocalRef(arrayListClass);
	env->DeleteLocalRef(questionAnswerClass);

	// Visszaadjuk a feltöltött Java ArrayList objektumot
	// Return the filled Java ArrayList object
	return resultList;
}
