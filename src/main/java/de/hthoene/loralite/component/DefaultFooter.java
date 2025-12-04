package de.hthoene.loralite.component;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.hthoene.loralite.util.ApplicationInformation;

public class DefaultFooter extends HorizontalLayout {
    public DefaultFooter() {
        setAlignItems(Alignment.CENTER);
        Span text = new Span("LoRA-Lite %s".formatted(ApplicationInformation.VERSION));
        Span author = new Span("by Hannes Thöne");
        add(text, author);
    }
}
