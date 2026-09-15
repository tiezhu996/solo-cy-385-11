package com.babytracker.dto;

import com.babytracker.entity.Baby;
import lombok.Data;
import java.time.LocalDate;

/** 宝宝档案视图，附带当前用户在家庭中的角色。 */
@Data
public class BabyView {
    private Long id;
    private String name;
    private LocalDate birthday;
    private String bloodType;
    private Double initialHeight;
    private Double initialWeight;
    private Long createdBy;
    private String role;

    public static BabyView of(Baby baby, String role) {
        BabyView view = new BabyView();
        view.setId(baby.getId());
        view.setName(baby.getName());
        view.setBirthday(baby.getBirthday());
        view.setBloodType(baby.getBloodType());
        view.setInitialHeight(baby.getInitialHeight());
        view.setInitialWeight(baby.getInitialWeight());
        view.setCreatedBy(baby.getCreatedBy());
        view.setRole(role);
        return view;
    }
}
