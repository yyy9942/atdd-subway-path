package nextstep.subway.section;

import nextstep.subway.station.Station;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Embeddable
public class Sections {

    @OneToMany(cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "line_id")
    private List<Section> sections = new ArrayList<>();

    public Sections() {
    }

    public Sections(List<Section> lineStations) {
        this.sections = lineStations;
    }

    public List<Section> getSections() {
        return sections;
    }

    protected void addOnlyList(Section section) {
        this.sections.add(section);
    }

    private SectionAddStrategy findAddStrategy(Section section) {
        if (sections.isEmpty()) {
            return new EmptyStrategy(this);
        }

        // 상행역끼리 일치시 (구간 자르기)
        if (findByUpSection(section.getUpStation()).isPresent()) {
            return new DivideEqUpStationStrategy(this);
        }

        // 하행역이 동일한 상태로 상행이 다를 시 (구간 자르기)
        if (findByDownSection(section.getDownStation()).isPresent()) {
            return new DivideEqDownStationStrategy(this);
        }

        // 추가구간의 상행과 기존 하행역이 일치시
        if (findByDownSection(section.getUpStation()).isPresent()) {
            return new AddLastStrategy(this);
        }

        // 추가 구간의 하행과 기존 상행이 일치시 (새로운 상행 추가)
        if (findByUpSection(section.getDownStation()).isPresent()) {
            return new AddFirstStrategy(this);
        }

        throw new IllegalArgumentException("추가할 수 있는 조건이 아닙니다");
    }

    public List<Section> addSection(Section section) {
        this.findAddStrategy(section).add(section);
        return sections;
    }

    public List<Station> getStations() {
        final List<Station> stations = new ArrayList<>();
        if (sections.isEmpty()) {
            return stations;
        }

        Section cursor = getFirstSection();

        stations.add(cursor.getUpStation());
        stations.add(cursor.getDownStation());

        while(hasNextSection(cursor)) {
            cursor = findByUpSection(cursor.getDownStation()).orElseThrow();
            stations.add(cursor.getDownStation());
        }
        return stations;
    }

    public long countOfStations() {
        return sections.size();
    }

    public Optional<Section> findByUpSection(final Station station) {
        return this.sections
            .stream().filter(e -> e.isUpStation(station))
            .findFirst();
    }

    public Optional<Section> findByDownSection(final Station station) {
        return this.sections
            .stream().filter(e -> e.isDownStation(station))
            .findFirst();
    }

    public Section getFirstSection() {
        return this.sections.stream()
            .filter(e -> this.findByDownSection(e.getUpStation()).isEmpty())
            .findFirst()
            .orElse(null);
    }

    public boolean hasNextSection(Section section) {
        return findByUpSection(section.getDownStation()).isPresent();
    }

    @Override
    public String toString() {
        return "Sections{" +
            "sections=" + sections +
            '}';
    }
}
