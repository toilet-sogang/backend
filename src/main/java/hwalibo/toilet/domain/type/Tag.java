package hwalibo.toilet.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Tag {

    // 긍정적 태그
    TOILET_CLEAN("변기 상태가 청결해요"),
    SINK_CLEAN("세면대가 청결해요"),
    GOOD_VENTILATION("환기가 잘 돼요"),
    ENOUGH_HANDSOAP("손 세정제가 충분해요"),
    BRIGHT_LIGHTING("조명이 밝아요"),

    // 부정적 태그
    TRASH_OVERFLOW("쓰레기가 넘쳐요"),
    DIRTY_FLOOR("바닥이 더러워요"),
    DIRTY_MIRROR("거울이 지저분해요"),
    NO_TOILET_PAPER("휴지가 없어요"),
    BAD_ODOR("악취가 심해요");

    private final String description;
}