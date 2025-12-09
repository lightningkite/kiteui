read -r -d '' prompt <<EOF
I need you to review $1 as an expert Kotlin library engineer would.
Do *NOT* modify the code in the files you review - only add comments.

Please do the following:

- Review the given code and its actuals for any obvious errors.  If you think you've found an error, place a TODO comment above the code with the issue.
- Note the usage of deprecated APIs as a TODO, but leave the deprecated APIs themselves alone.
- Update or create doc comments.  Please keep your comments concise, but ensure you include any 'gotchas' you might notice.
- Update or create unit tests you think would be useful.  You may change the visibility of constructs to create your tests.  Run your tests to check that they pass - if they don't and you believe that indicates an issue with the original code, add '@Ignore' to the test and add it as a TODO comment at the bottom of the original file to fix the problem in question.
- At the bottom of each file, if you have any recommendations for improving the API of the file, please enter them in a multi-line comment as a TODO.  Only add recommendations that you feel are important and are relevant to the API, not the implementation.  Especially note recommendations that center on overall design and likely user needs.
- Update or create relevant documentation in the /docs folder, ensuring it is accurate.
  - Good documentation in the /docs folder focuses on the basics of using the content in question.  It should be written in a way that a developer who is not familiar with the library can understand.

Take your time and get these right.  Thank you!
EOF
echo $prompt
claude --permission-mode acceptEdits "$prompt"
echo $?
