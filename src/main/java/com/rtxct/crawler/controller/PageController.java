package com.rtxct.crawler.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.rtxct.crawler.bot.Bot;
import com.rtxct.crawler.dto.PageDTO;
import com.rtxct.crawler.model.BotModel;

@RestController
@RequestMapping("/api/crawler")
public class PageController {

  @PostMapping("/")
  @ResponseBody
  public List<PageDTO> crawler(@RequestBody BotModel botModel) {
    return new Bot(botModel.getUrls()).crawlSync();
  }

  @PostMapping("/breakpoint/{breakpoint}")
  @ResponseBody
  public List<PageDTO> crawlerBreak(@RequestBody BotModel botModel, @PathVariable Integer breakpoint) {
    return new Bot(botModel.getUrls(), breakpoint).crawlSync();
  }

  @PostMapping("/async")
  @ResponseBody
  public List<PageDTO> asyncCrawling(@RequestBody BotModel botModel) {
    return new Bot(botModel.getUrls()).crawlAsync();
  }

  @PostMapping("/async/breakpoint/{breakpoint}")
  @ResponseBody
  public List<PageDTO> asyncCrawlingBreak(@RequestBody BotModel botModel, @PathVariable Integer breakpoint) {
    return new Bot(botModel.getUrls(), breakpoint).crawlAsync();
  }

  @PostMapping("/async/breakpoint/{breakpoint}/cores/{cores}")
  @ResponseBody
  public List<PageDTO> asyncCrawlingBreakAndCors(@RequestBody BotModel botModel, @PathVariable Integer breakpoint,
      @PathVariable Integer cores) {
    return new Bot(botModel.getUrls(), breakpoint).crawlAsync(cores);
  }
}
