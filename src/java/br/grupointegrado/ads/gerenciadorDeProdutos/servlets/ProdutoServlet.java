package br.grupointegrado.ads.gerenciadorDeProdutos.servlets;

import br.grupointegrado.ads.gerenciadorDeProdutos.modelos.Produto;
import br.grupointegrado.ads.gerenciadorDeProdutos.modelos.ProdutoDao;
import br.grupointegrado.ads.gerenciadorDeProdutos.utils.Formatter;
import br.grupointegrado.ads.gerenciadorDeProdutos.utils.Validations;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.tomcat.util.http.fileupload.FileUploadException;

/**
 * URL: /produtos
 *
 * @author João Gabriel
 */
public class ProdutoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        Connection conexao = (Connection) req.getAttribute("conexao");

        ProdutoDao dao = new ProdutoDao(conexao);

        long produtoId = Formatter.stringParaLong(req.getParameter("produto"));
        long excluirProdutoId = Formatter.stringParaLong(req.getParameter("excluirProduto"));

        if (excluirProdutoId > 0) {
            try {
                // excluir o produto do banco de dados.
                dao.remover(excluirProdutoId);
                resp.sendRedirect("/gerenciador/produtos");
            } catch (Exception ex) {
                ex.printStackTrace();
                req.setAttribute("mensagem-erro", "Não foi possível excluir o produto.");

                listarProdutos(req, resp);
            }
        } else {
            // Verifica se o ID do produto foi informado na URL
            if (produtoId > 0) {
                Produto produtoEncontrado = null;
                try {
                    produtoEncontrado = dao.buscaPorId(produtoId);
                    if (produtoEncontrado != null) {
                        // Se o produto existe, então devolve o produto para a JSP
                        req.setAttribute("produto", produtoEncontrado);
                    } else {
                        // Se não, exibe uma mensagem de erro
                        req.setAttribute("mensagem-erro", "Produto não encontrado.");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    req.setAttribute("mensagem-erro", "Náo foi possível buscar o produto.");
                }
            }
            listarProdutos(req, resp);
        }
    }

    private void listarProdutos(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        /**
         * 1. Fazer a consulta dos produtos a serem exibidos. <br />
         * 2. Montar uma listta com os produtos que serão apresentados na página
         * JSP. <br />
         * 3. Encaminhar a requisição para a página JSP apresentar o "response".
         */

        Connection conexao = (Connection) req.getAttribute("conexao");

        ProdutoDao dao = new ProdutoDao(conexao);

        String buscarProduto = req.getParameter("buscar-produto");

        List<Produto> produtos = new ArrayList<>();

        try {
            produtos = dao.buscaTodos(buscarProduto);
        } catch (Exception ex) {
            ex.printStackTrace();
            req.setAttribute("mensagem-erro", "Não foi possível conectar com o banco de dados.");
        }

        req.setAttribute("produtos", produtos);

        RequestDispatcher dispatcher = req.getRequestDispatcher("/WEB-INF/paginas/produtos.jsp");
        dispatcher.forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        /**
         * 1. Recuperar os parâmetros do formulário. <br>
         * 2. Criar um objeto produto. <br>
         * 3. Salvar o Produto no banco de dados. <br>
         * 4. Exibir a página de listagem atualizada.
         */

        try {
            Produto produto = ProdutoDao.getProdutoByRequest(req);
            String mensagemErro = validaCadastro(produto);

            if (mensagemErro == null) {
                Connection conexao = (Connection) req.getAttribute("conexao");
                // Os dados do produto são válidos
                ProdutoDao dao = new ProdutoDao(conexao);
                 try {
                    if (produto.getId() > 0) {
                        // Se o produto já possui ID, então deve atualizar
                        dao.atualizar(produto);
                    } else {
                        // Se não, inserir novo produto
                        dao.inserir(produto);
                    }
                    resp.sendRedirect("/gerenciador/produtos");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    req.setAttribute("mensagem-erro", "Não foi possível salvar o produto.");
                    req.setAttribute("produto", produto);
                     listarProdutos(req, resp);
            
                }
                 
                 } else {
                // Os dados do produto são inválidos
                req.setAttribute("mensagem-erro", mensagemErro);
                    
                    
                req.setAttribute("produto", produto);
                listarProdutos(req, resp);
            }
        } catch (FileUploadException ex) {
            ex.printStackTrace();
            req.setAttribute("mensagem-erro", "Não foi possível salvar a imagem do produto.");

            listarProdutos(req, resp);
        }

    }

    private String validaCadastro(Produto produto) {
        if (!Validations.validaString(produto.getNome(), 5)) {
            return "O nome do produto deve possuir ao menos 5 caracteres.";
        }
        
        if (!Validations.validaDouble(produto.getPreco(), 0.01, Double.MAX_VALUE)) {
            return "O preço do produto é obrigatório.";
        }
        
        if (!Validations.validaLong(produto.getQuantidade(), 0, Integer.MAX_VALUE)) {
            return "A quantidade do produto é obrigatória.";
        }
        
        if (produto.getId() == 0 && produto.getImagem() == null) {
            return "A imagem do produto é obrigatória.";
        }

        Calendar calendar = GregorianCalendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date dataMinima = calendar.getTime();

        calendar.add(Calendar.YEAR, 10);
        Date dataMaxima = calendar.getTime();

         if (!Validations.validaData(produto.getDataValidade(), dataMinima, dataMaxima)) {
            return "Informe a data de validade de produto.";
        }
        return null;
    }
}
