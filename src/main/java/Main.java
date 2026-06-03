import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedHashMap;
import java.util.Map;

public class Main {

    static class Lote {
        String nome;
        double margem;
        double horas;
        double cobre;
        double plastico;
        int demandaMaxima;

        Lote(String nome, double margem, double horas, double cobre, double plastico, int demandaMaxima) {
            this.nome = nome;
            this.margem = margem;
            this.horas = horas;
            this.cobre = cobre;
            this.plastico = plastico;
            this.demandaMaxima = demandaMaxima;
        }
    }

    public static void main(String[] args) {
        Map<String, Lote> lotes = lerEConsolidarCsv("dados.csv");

        System.out.println("DADOS CONSOLIDADOS");
        for (Lote lote : lotes.values()) {
            System.out.printf(
                    "%-25s margem=%6.2f horas=%5.2f cobre=%6.2f plastico=%6.2f demanda=%d%n",
                    lote.nome,
                    lote.margem,
                    lote.horas,
                    lote.cobre,
                    lote.plastico,
                    lote.demandaMaxima
            );
        }

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Map<String, Variable> variaveis = new LinkedHashMap<>();

        for (Lote lote : lotes.values()) {
            Variable variavel = model.addVariable(lote.nome)
                    .lower(0)
                    .upper(lote.demandaMaxima)
                    .integer(true)
                    .weight(lote.margem);

            variaveis.put(lote.nome, variavel);
        }

        Expression limiteHoras = model.addExpression("Limite de horas de desmontagem")
                .upper(540);

        Expression limiteCobre = model.addExpression("Capacidade de separacao de cobre")
                .upper(6800);

        Expression limitePlastico = model.addExpression("Capacidade de separacao de plastico")
                .upper(9200);

        for (Lote lote : lotes.values()) {
            Variable variavel = variaveis.get(lote.nome);

            limiteHoras.set(variavel, lote.horas);
            limiteCobre.set(variavel, lote.cobre);
            limitePlastico.set(variavel, lote.plastico);
        }

        Variable desktop = variaveis.get("Computadores Desktop");
        Variable impressora = variaveis.get("Impressoras");
        Variable monitor = variaveis.get("Monitores LCD");
        Variable notebook = variaveis.get("Notebooks");
        Variable roteador = variaveis.get("Roteadores e Modems");

        model.addExpression("Minimo de roteadores e modems")
                .lower(6)
                .set(roteador, 1);

        model.addExpression("Notebooks nao podem ultrapassar desktops")
                .lower(0)
                .set(desktop, 1)
                .set(notebook, -1);

        model.addExpression("Lotes pesados devem representar pelo menos 45 por cento do total")
                .lower(0)
                .set(desktop, 55)
                .set(impressora, 55)
                .set(monitor, 55)
                .set(notebook, -45)
                .set(roteador, -45);

        Optimisation.Result resultado = model.maximise();

        int qtdDesktop = desktop.getValue().intValue();
        int qtdImpressora = impressora.getValue().intValue();
        int qtdMonitor = monitor.getValue().intValue();
        int qtdNotebook = notebook.getValue().intValue();
        int qtdRoteador = roteador.getValue().intValue();

        int totalLotes = qtdDesktop + qtdImpressora + qtdMonitor + qtdNotebook + qtdRoteador;
        int totalPesados = qtdDesktop + qtdImpressora + qtdMonitor;

        double margemTotal =
                qtdDesktop * lotes.get("Computadores Desktop").margem +
                        qtdImpressora * lotes.get("Impressoras").margem +
                        qtdMonitor * lotes.get("Monitores LCD").margem +
                        qtdNotebook * lotes.get("Notebooks").margem +
                        qtdRoteador * lotes.get("Roteadores e Modems").margem;

        double horasUsadas =
                qtdDesktop * lotes.get("Computadores Desktop").horas +
                        qtdImpressora * lotes.get("Impressoras").horas +
                        qtdMonitor * lotes.get("Monitores LCD").horas +
                        qtdNotebook * lotes.get("Notebooks").horas +
                        qtdRoteador * lotes.get("Roteadores e Modems").horas;

        double cobreUsado =
                qtdDesktop * lotes.get("Computadores Desktop").cobre +
                        qtdImpressora * lotes.get("Impressoras").cobre +
                        qtdMonitor * lotes.get("Monitores LCD").cobre +
                        qtdNotebook * lotes.get("Notebooks").cobre +
                        qtdRoteador * lotes.get("Roteadores e Modems").cobre;

        double plasticoUsado =
                qtdDesktop * lotes.get("Computadores Desktop").plastico +
                        qtdImpressora * lotes.get("Impressoras").plastico +
                        qtdMonitor * lotes.get("Monitores LCD").plastico +
                        qtdNotebook * lotes.get("Notebooks").plastico +
                        qtdRoteador * lotes.get("Roteadores e Modems").plastico;

        double percentualPesados = (totalPesados * 100.0) / totalLotes;

        System.out.println();
        System.out.println("RESULTADO DA OTIMIZACAO");
        System.out.println("Status da solucao: " + resultado.getState());

        System.out.println();
        System.out.println("Quantidade ideal de lotes:");
        System.out.println("Computadores Desktop: " + qtdDesktop);
        System.out.println("Impressoras: " + qtdImpressora);
        System.out.println("Monitores LCD: " + qtdMonitor);
        System.out.println("Notebooks: " + qtdNotebook);
        System.out.println("Roteadores e Modems: " + qtdRoteador);

        System.out.println();
        System.out.printf("Margem maxima: R$ %.2f%n", margemTotal);

        System.out.println();
        System.out.println("Uso dos recursos:");
        System.out.printf("Horas usadas: %.0f de 540%n", horasUsadas);
        System.out.printf("Cobre usado: %.0f kg de 6800 kg%n", cobreUsado);
        System.out.printf("Plastico usado: %.0f kg de 9200 kg%n", plasticoUsado);

        System.out.println();
        System.out.println("Regras adicionais:");
        System.out.println("Total de lotes processados: " + totalLotes);
        System.out.println("Total de lotes pesados: " + totalPesados);
        System.out.printf("Percentual de lotes pesados: %.2f%%%n", percentualPesados);
    }

    private static Map<String, Lote> lerEConsolidarCsv(String caminho) {
        Map<String, Lote> lotes = new LinkedHashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(caminho))) {
            br.readLine();

            String linha;
            while ((linha = br.readLine()) != null) {
                if (linha.isBlank()) {
                    continue;
                }

                String[] campos = linha.split(";");

                String nome = campos[0].trim();
                double margem = Double.parseDouble(campos[1].trim().replace(",", "."));
                double horas = Double.parseDouble(campos[2].trim().replace(",", "."));
                double cobre = Double.parseDouble(campos[3].trim().replace(",", "."));
                double plastico = Double.parseDouble(campos[4].trim().replace(",", "."));
                int demandaMaxima = (int) Math.round(Double.parseDouble(campos[5].trim().replace(",", ".")));

                if (lotes.containsKey(nome)) {
                    Lote loteExistente = lotes.get(nome);
                    loteExistente.demandaMaxima += demandaMaxima;
                } else {
                    lotes.put(nome, new Lote(nome, margem, horas, cobre, plastico, demandaMaxima));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao ler o arquivo CSV: " + caminho, e);
        }

        return lotes;
    }
}