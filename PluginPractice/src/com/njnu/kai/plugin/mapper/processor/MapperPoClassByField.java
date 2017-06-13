package com.njnu.kai.plugin.mapper.processor;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.njnu.kai.plugin.mapper.model.WaitPOItem;
import com.njnu.kai.plugin.util.PsiFileUtils;
import com.njnu.kai.plugin.util.Utils;
import org.jetbrains.annotations.NotNull;

/**
 * @author hongkai.qian
 * @version 1.0.0
 * @since 16-7-29
 */
public class MapperPoClassByField {

    private Project mProject;
    private MapperPoClassListener mMapperPoClassListener;
    private WaitPOItem mWaitPOItem;

    private PsiElementFactory mFactory;
    private PsiClass mPoClass;
    private PsiClass mVoClass;
    private PsiClass mMapperClass;

    private static String OBJECT_TRANSFORM_METHOD = "/**\n" +
            "        * @param po persist object\n" +
            "        * @return view object\n" +
            "        */\n" +
            "        public static $MAPPED_CLASS$ transform($ORIGIN_CLASS$ po) {\n" +
            "        if (null == po) {\n" +
            "        return null;\n" +
            "        }\n\n" +
            "        $MAPPED_CLASS$  vo = new $MAPPED_CLASS$();\n\n" +
            "        $BODY$\n" +
            "        return vo;\n" +
            "    }\n\n";

    private static String OBJECT_LIST_TRANSFORM_METHOD = "/**\n" +
            "        * @param poList list of persist object \n" +
            "        * @return list of view object \n" +
            "        */ \n" +
            "        public static List<$MAPPED_CLASS$> transform$ENTITY$List(List<$ORIGIN_CLASS$> poList) {\n" +
            "        List<$MAPPED_CLASS$>  voList = new ArrayList<>();\n\n" +
            "        if (poList != null) {" +
            "        for ($ORIGIN_CLASS$ po : poList) {\n" +
            "        if (po != null) {\n" +
            "        voList.add(transform(po));\n" +
            "        }\n" +
            "        }\n" +
            "        }\n" +
            "        return voList;\n" +
            "    }\n\n";

    private static String METHOD_BODY_ITEM = "vo.$PROPERTY$ = po.$GETTER$;\n";
    private static String METHOD_BODY_PO_ITEM = "vo.$PROPERTY$ = transform(po.$GETTER$);\n";
    private static String METHOD_BODY_ITEM_LIST = "vo.$PROPERTY$ = po.$GETTER$;\n";
    private static String METHOD_BODY_PO_ITEM_LIST = "vo.$PROPERTY$ = transform$ENTITY$List(po.$GETTER$);\n";

    public MapperPoClassByField(Project project, MapperPoClassListener mapperPoClassListener, WaitPOItem waitPOItem) {
        mProject = project;
        mMapperPoClassListener = mapperPoClassListener;
        mWaitPOItem = waitPOItem;
        mFactory = JavaPsiFacade.getElementFactory(mProject);
    }

    public void execute(boolean newMapperClass) {
        mPoClass = mWaitPOItem.getPoClass();
        mVoClass = PsiFileUtils.createClass(mProject, mWaitPOItem.getVoClassCanonicalName(), false);
        mMapperClass = PsiFileUtils.createClass(mProject, mWaitPOItem.getMapperClassCanonicalName(), newMapperClass);
        generateObjectClass();
        generateMapperClass();
    }

    private void generateObjectClass() {
        generateFields(mVoClass);
        PsiFileUtils.optimizeStyle(mProject, mVoClass);
    }

    private void generateMapperClass() {
        generateImports(mMapperClass);
        generateObjectTransformMethod(mMapperClass);
        generateObjectListTransformMethod(mMapperClass);
        PsiFileUtils.optimizeStyle(mProject, mMapperClass);
    }

    private void generateObjectTransformMethod(PsiClass mapperClass) {
        String originClassName = mPoClass.getName();
        String mappedClassName = mVoClass.getName();

        String methodText = OBJECT_TRANSFORM_METHOD.replace("$ORIGIN_CLASS$", originClassName)
                .replace("$MAPPED_CLASS$", mappedClassName)
                .replace("$ENTITY$", Utils.getClassEntityName(originClassName))
                .replace("$BODY$", methodBody());
//        System.out.println("methodText is: " + methodText);
        mapperClass.add(mFactory.createMethodFromText(methodText, mapperClass));
    }

    private void generateObjectListTransformMethod(PsiClass mapperClass) {
        String originClassName = mPoClass.getName();
        String mappedClassName = mVoClass.getName();

        String methodText = OBJECT_LIST_TRANSFORM_METHOD.replace("$ORIGIN_CLASS$", originClassName)
                .replace("$ENTITY$", Utils.getClassEntityName(originClassName))
                .replace("$MAPPED_CLASS$", mappedClassName);
        mapperClass.add(mFactory.createMethodFromText(methodText, mapperClass));
    }

    private void generateImports(PsiClass mapperClass) {
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(mProject);
        PsiJavaFile file = (PsiJavaFile) mapperClass.getContainingFile();

        styleManager.addImport(file, mPoClass);
        styleManager.addImport(file, mVoClass);

        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(mProject);
        GlobalSearchScope searchScope = GlobalSearchScope.allScope(mProject);
        styleManager.addImport(file, javaPsiFacade.findClass("java.util.List", searchScope));
        styleManager.addImport(file, javaPsiFacade.findClass("java.util.ArrayList", searchScope));
    }

    private void addImport(PsiClass whichClass, String canonicalText) {
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(mProject);
        PsiJavaFile file = (PsiJavaFile) whichClass.getContainingFile();
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(mProject);
        GlobalSearchScope searchScope = GlobalSearchScope.allScope(mProject);
        styleManager.addImport(file, javaPsiFacade.findClass(canonicalText, searchScope));
    }

    private String methodBody() {
        StringBuilder methodBody = new StringBuilder();

        for (PsiField field : mPoClass.getFields()) {
            if (field.hasModifierProperty(PsiModifier.STATIC)) {
                continue;
            }

            String fieldName = field.getName();
            final String canonicalText = field.getType().getCanonicalText();

            if (canonicalText.startsWith(Utils.JAVA_LIST_TYPE)) {
                String itemCanonicalText = Utils.getListInnerType(canonicalText);
                final String amendFieldName = Utils.amendListFieldName(fieldName);
                if (Utils.isJavaInnerClass(itemCanonicalText)) {
                    final String methodItem = METHOD_BODY_ITEM_LIST
                            .replace("$PROPERTY$", amendFieldName)
                            .replace("$GETTER$", fieldName);
                    methodBody.append(methodItem);
                } else {
                    final String methodItem = METHOD_BODY_PO_ITEM_LIST
                            .replace("$PROPERTY$", amendFieldName)
                            .replace("$ENTITY$", Utils.getClassEntityName(itemCanonicalText))
                            .replace("$GETTER$", fieldName);
                    methodBody.append(methodItem);
                }
            } else {
                final String amendFieldName = Utils.amendFieldName(fieldName);
                if (Utils.isJavaInnerClass(canonicalText)) {
                    final String methodItem = METHOD_BODY_ITEM
                            .replace("$PROPERTY$", amendFieldName)
                            .replace("$GETTER$", fieldName);
                    methodBody.append(methodItem);
                } else {
                    final String methodItem = METHOD_BODY_PO_ITEM
                            .replace("$PROPERTY$", amendFieldName)
                            .replace("$GETTER$", fieldName);
                    methodBody.append(methodItem);
                }
            }
        }

        return methodBody.toString();
    }


    private static final String FIELD_ITEM_LIST = "public java.util.List<%s> %s;"; //原来的field已经有前缀如m,不用再加了
    private static final String FIELD_ITEM = "public %s %s;";

    private void generateFields(PsiClass objectClass) {
        for (PsiField field : mPoClass.getFields()) {
            if (!field.hasModifierProperty(PsiModifier.STATIC)) {
                final String name = field.getName();
                PsiType fieldType = field.getType();
                final String canonicalText = fieldType.getCanonicalText();
                if (canonicalText.startsWith(Utils.JAVA_LIST_TYPE)) {
                    String itemCanonicalText = Utils.getListInnerType(canonicalText);
                    if (Utils.isJavaInnerClass(itemCanonicalText)) {
                        String filedStr = String.format(FIELD_ITEM_LIST, itemCanonicalText, Utils.amendListFieldName(name));
                        objectClass.add(mFactory.createFieldFromText(filedStr, objectClass));
                    } else {
                        final String voTypeCanonicalName = getVoFieldTypeNameFromPoText(itemCanonicalText);
                        String filedStr = String.format(FIELD_ITEM_LIST, voTypeCanonicalName, Utils.amendListFieldName(name));
                        objectClass.add(mFactory.createFieldFromText(filedStr, objectClass));
                        mMapperPoClassListener.notifyFoundFieldInPoClass(itemCanonicalText);
                    }
                } else if (Utils.isJavaInnerClass(canonicalText)) {
                    String filedStr = String.format(FIELD_ITEM, fieldType.getCanonicalText(), name);
                    objectClass.add(mFactory.createFieldFromText(filedStr, objectClass));
                } else {
                    final String voTypeCanonicalName = getVoFieldTypeNameFromPoField(field);
                    String filedStr = String.format(FIELD_ITEM, voTypeCanonicalName, Utils.amendFieldName(name));
                    objectClass.add(mFactory.createFieldFromText(filedStr, objectClass));
                    mMapperPoClassListener.notifyFoundFieldInPoClass(field);
                }
            }
        }
    }

    private String getVoFieldTypeNameFromPoField(PsiField field) {
        final String canonicalText = field.getType().getCanonicalText();
        return getVoFieldTypeNameFromPoText(canonicalText);
    }

    @NotNull
    private String getVoFieldTypeNameFromPoText(String canonicalText) {
        return Utils.replaceFullPkgWithGivenClass(mWaitPOItem.getVoClassCanonicalName(), canonicalText) + "VO";
    }

    private void addNormalMethod(PsiClass objectClass, PsiMethod method) {
        final String oriMethodText = method.getText();
        String methodText = oriMethodText.replace("\b" + mPoClass.getName() + "\b", objectClass.getName());
        methodText = methodText.replace("this.", "");
        objectClass.add(mFactory.createMethodFromText(methodText, objectClass));
    }

}
