# PageDown

PageDown is the JavaScript [Markdown](http://daringfireball.net/projects/markdown/) previewer used on [Stack Overflow](http://stackoverflow.com/) and the rest of the [Stack Exchange network](http://stackexchange.com/). It includes a Markdown-to-HTML converter and an in-page Markdown editor with live preview. 

While on the Stack Exchange sites PageDown is exclusively used on the client site (on the server side, [MarkdownSharp](http://code.google.com/p/markdownsharp/) is our friend), PageDown's converter also works in a server environment. So if your Node.JS application lets users enter Markdown, you can also use PageDown to convert it to HTML on the server side. 

The largest part is based on work by John Fraser, a.k.a. Attacklab. He created the converter under the name _Showdown_ and the editor under the name _WMD_. See [this post on the Stack Exchange blog](http://blog.stackoverflow.com/2008/12/reverse-engineering-the-wmd-editor/) for some historical information. 

Over the years, we (the Stack Exchange team and others) have made quite a few changes, bug fixes etc., which is why we decided to publish the whole thing under a new name. This is not to mean we want to deprive John Fraser of any credits; he deserves lots. And you'll still be finding the letters &quot;wmd&quot; in a few places. 

It should be noted that Markdown is **not safe as far as user-entered input goes**. Pretty much anything is valid in Markdown, in particular something like <tt>&lt;script&gt;doEvil();&lt;/script&gt;</tt>. This PageDown repository includes the two plugins that Stack Exchange uses to sanitize the user's input; see the description of Markdown.Sanitizer.js below. 
