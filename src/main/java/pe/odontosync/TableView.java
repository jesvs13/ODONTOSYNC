package pe.odontosync;

import java.util.List;

public record TableView(String title, String subtitle, List<String> headers, List<List<String>> rows) { }
