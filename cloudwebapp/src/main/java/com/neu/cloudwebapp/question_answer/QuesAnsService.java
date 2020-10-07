package com.neu.cloudwebapp.question_answer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QuesAnsService {

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    AnswerRepository answerRepository;

    public List<Object> getAllQuestions(List<Question> questions) {

        List<Object> result = new ArrayList<>();

        for (Question q : questions) {
            result.add(getQuestion(q.getQuestion_id(), 0));
        }

        return result;
    }

    public HashMap<String, Object> getQuestion(UUID uuid, int i) {
        try{

            HashMap<String, Object> obj = new HashMap<>();
            Optional<Question> question = questionRepository.findById(uuid);

            obj.put("question_id", question.get().getQuestion_id().toString());
            obj.put("question_text", question.get().getQuestion_text());
            obj.put("created_timestamp", question.get().getCreated_timestamp());
            obj.put("updated_timestamp", question.get().getUpdated_timestamp());
            obj.put("user_id", question.get().getUser().getId());
            obj.put("categories", question.get().getCategories());

            if(i == 1) {
                obj.put("answers", question.get().getAnswers());
            } else {
                List<HashMap<String, Object>> answerList = new ArrayList<>();

                for (Answer a : question.get().getAnswers() ) {
                    answerList.add(getAnswer(a.getAnswer_id()));
                }

                obj.put("answers", answerList);
            }

            return obj;

        }
        catch(Exception ex){
            System.out.println(ex);
            return null;
        }
    }

    public HashMap<String, Object> getAnswer(UUID uuid) {
        try {
            HashMap<String, Object> obj = new HashMap<>();

            Optional<Answer> ans = answerRepository.findById(uuid);

            obj.put("answer_id", ans.get().getAnswer_id().toString());
            obj.put("answer_text", ans.get().getAnswer_text());
            obj.put("created_timestamp", ans.get().getCreated_timestamp());
            obj.put("updated_timestamp", ans.get().getUpdated_timestamp());
            obj.put("user_id", ans.get().getUser().getId());
            obj.put("question_id", ans.get().getQuestion().getQuestion_id());

            return obj;
        }
        catch(Exception ex){
            System.out.println(ex);
            return null;
        }
    }


    public Category checkCategory(String cat) {

        Category c = categoryRepository.findByCategoryName(cat);

        if(c == null) {
            return null;
        }

        return c;
    }

}
